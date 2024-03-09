package jdbc.stream.demo;

import lombok.SneakyThrows;
import lombok.extern.java.Log;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;

@Log
public class GenerateDataSqlTest {

    @SneakyThrows
    @Test
    public void generateInitDataSql() {
        int loop = 20000;
        StringBuilder sb = new StringBuilder("INSERT INTO public.user (user_id, pass_word, roles, introduction) VALUES ");
        for (int i = 0; i < loop; i++) {
            sb.append("( ");
            sb.append(i+1);
            sb.append(",");
            sb.append("'");
            sb.append(RandomStringUtils.random(60, true, true));
            sb.append("'");
            sb.append(",");
            sb.append("'");
            sb.append(RandomStringUtils.random(200, true, true));
            sb.append("'");
            sb.append(",");
            sb.append("'");
            sb.append(RandomStringUtils.random(200, true, true));
            sb.append("'");
            sb.append(")");
            if (i +1 < loop) {
                sb.append(",");
            }
        }
        File file = new File("src/main/resources/META-INF/sql/data.sql");
        if (file.exists()) {
            file.delete();
        }
        file.createNewFile();
        try(FileWriter fileWriter = new FileWriter(file);) {
            fileWriter.append(sb.toString());
        }
    }
}
