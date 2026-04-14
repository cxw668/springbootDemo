package demo.common;

import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.engine.VelocityTemplateEngine;

import java.util.Collections;

public class CodeGenerator {
    public static void main(String[] args) {
        FastAutoGenerator.create(
                "jdbc:mysql://localhost:3306/demo?useUnicode=true&characterEncoding=utf8&serverTimezone=UTC",
                "root",
                "1234"
        )
                .globalConfig(builder -> {
                    builder.author("cxw_p") // 设置作者
                            .outputDir("src/main/java"); // 输出目录
                })
                .packageConfig(builder -> {
                    builder.parent("demo") // 设置父包名
                            .entity("model") // 设置实体类包名
                            .mapper("mapper") // 设置Mapper接口包名
                            .service("service") // 设置Service接口包名
                            .serviceImpl("service.impl") // 设置Service实现类包名
                            .controller("controller") // 设置Controller包名
                            .pathInfo(Collections.singletonMap(OutputFile.xml, "src/main/resources/mapper")); // 设置XML文件输出目录
                })
                .strategyConfig(builder -> {
                    builder.addInclude("user") // 设置需要生成的表名，多个表用逗号分隔
                            .entityBuilder()
                            .enableLombok() // 启用Lombok
                            .enableTableFieldAnnotation() // 启用字段注解
                            .controllerBuilder()
                            .enableRestStyle(); // 启用REST风格
                })
                .templateEngine(new VelocityTemplateEngine()) // 使用Velocity模板引擎
                .execute(); // 执行生成
    }
}
