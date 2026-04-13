/*
package zju.cst.aces.runner;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import io.joern.client.JoernClient;              // ① CPG
import trusttest.graphcodebert.GRAPH_ENCODER;   // ② 图编码
*/
/*
Joern 是一个用于对源代码、字节码和二进制代码进行可靠分析的平台。
它生成代码属性图,即代码的图形表示 跨语言代码分析。
代码属性图块将被存储在自定义图形数据库中。
这使得代码能够通过搜索进行挖掘 在基于 Scala 的特定域查询中制定的查询 语言。
Joern 旨在提供一种有用的工具 用于静态程序分析中的漏洞发现与研究。
 *//*


public class AnalyzeResultStorage2 {

    */
/* ================= 原有成员 ================= *//*

    private static final String BASE_STORAGE_DIR = "target";
    private Set<String> processedClasses = new HashSet<>();
    private String currentRootDir;
    private TestCoreDemo2 testCoreClass;

    */
/* ================= 新增成员 ================= *//*

    private JoernClient joern = new JoernClient("http://localhost:8080");
    private GRAPH_ENCODER graphEnc = GRAPH_ENCODER.load();

    public AnalyzeResultStorage2(String rootDir, TestCoreDemo2 testCore) {
        this.currentRootDir = rootDir;
        this.testCoreClass = testCore;
    }

    */
/* 入口：分析 + 存储 *//*

    public void startAnalyzeAndStore(String className, String sourceCodeDir) throws IOException {
        String rootStorageDir = Paths.get(BASE_STORAGE_DIR, "analyzeStart-test-" + currentRootDir).toString();
        File rootDir = new File(rootStorageDir);
        if (!rootDir.exists()) rootDir.mkdirs();

        */
/* ---- 一次性建 CPG ---- *//*

        if (joern.getCpg() == null) {
            System.out.println("[JOERN] 开始导入代码目录: " + sourceCodeDir);
            joern.importCode(sourceCodeDir);
            System.out.println("[JOERN] CPG 构建完成");
        }

        analyzeAndStoreRecursive(className, sourceCodeDir, rootStorageDir);
    }

    */
/* 递归分析：核心改动只在 prompt 处 *//*

    private void analyzeAndStoreRecursive(String className, String sourceCodeDir, String rootStorageDir) throws IOException {
        if (processedClasses.contains(className)) return;
        processedClasses.add(className);

        String classFilePath = findClassInDir(className, sourceCodeDir);
        if (classFilePath == null) {
            System.out.println("类[" + className + "]不在目录[" + sourceCodeDir + "]下，跳过");
            return;
        }

        String resultFilePath = Paths.get(rootStorageDir, className + ".analyze").toString();
        File resultFile = new File(resultFilePath);

        String analyzeResult;
        if (resultFile.exists()) {
            analyzeResult = Files.readString(resultFile.toPath());
            System.out.println("类[" + className + "]已存在，直接读取");
        } else {
            System.out.println("开始分析类[" + className + "]...");
            String sourceCode = Files.readString(Paths.get(classFilePath));

            */
/* ===== 拿到子图 ===== *//*

            String funcSig = className + ".*";
            var subGraph      = joern.slice(funcSig, 2);          // 2-hop
            String graphNL    = joern.toNL(subGraph);             // 自然语言摘要
            float[] graphVec  = graphEnc.encode(subGraph);        // 768 维向量
            */
/* ==================== *//*


            List<String> chunks = splitSourceCode(sourceCode);
            StringBuilder full = new StringBuilder();
            for (String chunk : chunks) {
                String rich = "[Graph Summary]\n" + graphNL + "\n" +
                        "[Graph Vector]\n" + vecToHex(graphVec) + "\n" +
                        "[Code]\n" + chunk;
                String out = testCoreClass.analyzeSourceCode(rich);
                if (out != null) full.append(out).append("\n");
            }
            analyzeResult = full.toString()
                    .replace("```", "")
                    .replace("```over", "")
                    .replaceAll("(?m)^\\s*$\\n?", "");
            if (analyzeResult.isEmpty()) {
                System.out.println("LLM 未返回有效结果，跳过");
                return;
            }
            Files.writeString(Paths.get(resultFilePath), analyzeResult);
            System.out.println("结果已写入: " + resultFilePath);
        }

        */
/* 递归依赖 & 父类（逻辑不变） *//*

        ClassDependencyInfo info = extractDependencyInfo(analyzeResult, className);
        for (String dep : info.getDependencies()) {
            analyzeAndStoreRecursive(dep, sourceCodeDir, rootStorageDir);
        }
        if (info.getSuperClass() != null && !info.getSuperClass().isEmpty()) {
            analyzeAndStoreRecursive(info.getSuperClass(), sourceCodeDir, rootStorageDir);
        }
    }

    */
/* ---------------- 以下全是原有辅助方法 ---------------- *//*

    private List<String> splitSourceCode(String sourceCode) {
        List<String> chunks = new ArrayList<>();
        int max = 5000, len = sourceCode.length();
        for (int i = 0; i < len; i += max) {
            int end = Math.min(i + max, len);
            int last = sourceCode.lastIndexOf("}", end);
            if (last > i) end = last + 1;
            chunks.add(sourceCode.substring(i, end));
        }
        return chunks;
    }

    private String findClassInDir(String className, String dir) {
        File[] fs = new File(dir).listFiles((d, n) -> n.equals(className + ".java"));
        return (fs != null && fs.length > 0) ? fs[0].getAbsolutePath() : null;
    }

    private ClassDependencyInfo extractDependencyInfo(String content, String className) {
        ClassDependencyInfo info = new ClassDependencyInfo();
        List<String> dep = new ArrayList<>();
        String superC = "";
        */
/* 父类 *//*

        String[] tmp = content.split("父类：");
        if (tmp.length > 1) superC = tmp[1].trim().split("\n")[0].replaceAll("[^a-zA-Z0-9]", "");
        */
/* 依赖类 *//*

        tmp = content.split("依赖类：");
        if (tmp.length > 1) {
            for (String s : tmp[1].trim().split("\n")[0].split(",")) {
                s = s.replaceAll("[^a-zA-Z0-9]", "");
                if (!s.isEmpty()) dep.add(s);
            }
        }
        info.setDependencies(dep);
        info.setSuperClass(superC);
        return info;
    }

    private static String vecToHex(float[] v) {
        byte[] b = new byte[v.length * 4];
        ByteBuffer.wrap(b).asFloatBuffer().put(v);
        return HexFormat.of().formatHex(b);   // 128 字节
    }

    private static class ClassDependencyInfo {
        private List<String> dependencies = new ArrayList<>();
        private String superClass = "";
        List<String> getDependencies() { return dependencies; }
        void setDependencies(List<String> d) { this.dependencies = d; }
        String getSuperClass() { return superClass; }
        void setSuperClass(String s) { this.superClass = s; }
    }

    */
/* ---------------- main：直接跑 ---------------- *//*

    public static void main(String[] args) throws IOException {
        String apiKey   = "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";
        String srcDir   = "src/main/java/cli";
        String rootName = "cli";
        String startCls = "BasicParser";

        TestCoreDemo2 core = new TestCoreDemo2(apiKey);
        new AnalyzeResultStorage2(rootName, core).startAnalyzeAndStore(startCls, srcDir);
    }
}*/
