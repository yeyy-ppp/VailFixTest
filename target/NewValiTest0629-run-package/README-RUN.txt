NewValiTest0629 运行包

当前项目验证环境：
- JDK: OpenJDK / Temurin 17.0.19
- Maven: Apache Maven 3.9.1
- Windows 11, amd64

另一台电脑建议准备：
1. 安装 JDK 17，并确认 java -version 可用。
2. 安装 Maven 3.9.x，并确认 mvn -version 可用。
3. 解压本压缩包到一个英文路径或无特殊字符路径，例如 D:\ValiRun\NewValiTest0629-run-package。

轻量检查命令：
cd D:\ValiRun\NewValiTest0629-run-package
java -jar chatunitest-core-1.1.0.jar --check-only --skip-coverage-run --output-root=D:\ValiRun\result-check

正式运行命令：
cd D:\ValiRun\NewValiTest0629-run-package
java -jar chatunitest-core-1.1.0.jar --output-root=D:\ValiRun\result0629

说明：
- 默认会读取当前运行目录下的 data0629 文件夹。
- 如果数据集放在其他位置，增加参数：--data-root=你的data0629路径
- 如果从其他目录启动 jar，增加参数：--project-root=本运行包解压目录
- 输出会保存到 --output-root 指定目录，里面包含 logs、test-code、coverage-reports 和 NewValiTest0629-summary.txt。
