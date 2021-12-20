import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;

public class MessageExporterMain {

	public static String OUTPUT_FILE = "./out/export.csv"; 																// some export file like './export.csv'
	public static String RESOURCE_ROOT_FOLDER_PATH = "C:\\Users\\domin\\Downloads\\MessageExporter\\resource";			// message~.properties root folder
	public static String DELIMITER = ",";
	public static String FILENAME_FORMAT = "messages_%s.properties";													// .properties 파일명 형태

	public static String[] LANGS = new String [] {
			"ko", "en"
	};

	public static Map<String, Properties> LOAD_PROPS = new HashMap<>();

	public static void main(String[] args) throws IOException {
		Path filePath = null;

		try {
			filePath = Files.write(Path.of(OUTPUT_FILE), new byte[0], StandardOpenOption.TRUNCATE_EXISTING);
		} catch (Exception e) {
			filePath = Files.write(Path.of(OUTPUT_FILE), new byte[0], StandardOpenOption.CREATE_NEW);
		}

		// 파일 출력
		PrintWriter writer = new PrintWriter(filePath.toFile());

		// 프로퍼티 파일 로딩
		for(String lang : LANGS) {
			Path messageFilePath = Path.of(RESOURCE_ROOT_FOLDER_PATH, String.format(FILENAME_FORMAT, lang));

			try ( InputStream is = new FileInputStream(messageFilePath.toFile()) ) {
				Properties prop = new Properties();
				prop.load(is);
				LOAD_PROPS.put(lang, prop);

			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		// 메시지 프로퍼티 간 키, 값 비교하여 병합
		Set<String> compareComplete = new HashSet<>();

		Map<String, Map<String, String>> mergeLines = new HashMap<>();

		for(Map.Entry<String, Properties> mainEntry : LOAD_PROPS.entrySet()) {
			String mainLang = mainEntry.getKey();
			Properties mainProps = mainEntry.getValue();

			Set<Object> mainKeySet = mainProps.keySet();

			for(Map.Entry<String, Properties> targetEntry : LOAD_PROPS.entrySet()) {
				String targetLang = targetEntry.getKey();
				Properties targetProps = targetEntry.getValue();

				if ( Objects.equals(mainEntry.getKey(),  targetEntry.getKey()) ) {
					continue;
				}

				if(compareComplete.contains(mainLang) && compareComplete.contains(targetLang)) {
					continue;
				}

				Set<Object> targetKeySet = targetProps.keySet();

				Set<Object> mergeKeySet = new HashSet<>(mainKeySet);
				mergeKeySet.addAll(targetKeySet);

				mergeKeySet.stream()
					.map(String::valueOf)
					.forEach(key -> {
						String mainValue = mainProps.getProperty(key, "");
						String targetValue = targetProps.getProperty(key, "");
						Map<String, String> mergeLine = mergeLines.getOrDefault(key, new HashMap<>());
						mergeLine.put(mainLang, mainValue);
						mergeLine.put(targetLang, targetValue);

						mergeLines.put(key, mergeLine);
					});

				compareComplete.add(mainLang);
				compareComplete.add(targetLang);
			}
		}

		// print colum
		List<String> columns = new ArrayList<>() {
			@Override
			public boolean add(String s) {
				return super.add("\"" + s + "\"");
			}
		};
		columns.add("key");
		Collections.addAll(columns, LANGS);

		String columnLine = String.join(DELIMITER, columns);
		writer.println(columnLine);
		System.out.println(columnLine);

		// print row
		for( Map.Entry<String, Map<String, String>> entry : mergeLines.entrySet()) {
			String key = entry.getKey();
			Map<String, String> value = entry.getValue();

			List<String> lineFormatArgs = new ArrayList<>() {
				@Override
				public boolean add(String s) {
					return super.add("\"" + s + "\"");
				}
			};
			lineFormatArgs.add(key);
			for(String lang : LANGS) {
				lineFormatArgs.add(value.getOrDefault(lang, "").replace("\\R", ""));
			}
			String line = String.join(DELIMITER, lineFormatArgs);
			System.out.println(line);
			writer.println(line);
		}

		writer.close();

	}

}
