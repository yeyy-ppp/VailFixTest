import zju.cst.aces.runner.CoverageEvaluator;
import java.util.*;

public class TestCoverageFilter {
    public static void main(String[] args) {
        // 模拟覆盖率数据
        List<Map<String, String>> coverageInfoList = new ArrayList<>();
        
        // 添加 cli 包的数据
        Map<String, String> cliData = new HashMap<>();
        cliData.put("Element", "cli");
        cliData.put("Instruction Coverage", "70.0%");
        coverageInfoList.add(cliData);
        
        // 添加 cli.help 子包的数据
        Map<String, String> cliHelpData = new HashMap<>();
        cliHelpData.put("Element", "cli.help");
        cliHelpData.put("Instruction Coverage", "85.0%");
        coverageInfoList.add(cliHelpData);
        
        // 添加其他包的数据
        Map<String, String> otherData = new HashMap<>();
        otherData.put("Element", "codeing");
        otherData.put("Instruction Coverage", "90.0%");
        coverageInfoList.add(otherData);
        
        // 测试过滤
        System.out.println("=== 测试 cli 包过滤（应包含 cli 和 cli.help）===");
        List<Map<String, String>> filtered = CoverageEvaluator.filterCoverageInfoByPackage(coverageInfoList, "cli");
        System.out.println("过滤结果数量: " + filtered.size() + " (期望: 2)");
        for (Map<String, String> item : filtered) {
            System.out.println("  - " + item.get("Element") + ": " + item.get("Instruction Coverage"));
        }
        
        System.out.println("\n=== 测试 codeing 包过滤 ===");
        filtered = CoverageEvaluator.filterCoverageInfoByPackage(coverageInfoList, "codeing");
        System.out.println("过滤结果数量: " + filtered.size() + " (期望: 1)");
        for (Map<String, String> item : filtered) {
            System.out.println("  - " + item.get("Element") + ": " + item.get("Instruction Coverage"));
        }
    }
}
