package zju.cst.aces.runner;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CoverageEvaluator {

    public static List<Map<String, String>> extractCoverageInfo(String jacocoReportPath) {
        List<Map<String, String>> coverageInfoList = new ArrayList<>();
        try {
            File input = new File(jacocoReportPath);
            Document doc = Jsoup.parse(input, "UTF-8");

            // 查找覆盖率表格
            Elements coverageTable = doc.select("table.coverage");

            if (!coverageTable.isEmpty()) {
                // 获取表格的tbody部分，包含各个元素的覆盖率行
                Elements tbody = coverageTable.first().select("tbody");
                if (!tbody.isEmpty()) {
                    Elements tbodyRows = tbody.first().select("tr");
                    for (Element row : tbodyRows) {
                        Elements rowData = row.select("td");

                        if (rowData.size() >= 13) {
                            Map<String, String> coverageInfo = new HashMap<>();

                            // 提取元素名称
                            coverageInfo.put("Element", rowData.get(0).text());

                            // 提取指令覆盖率
                            String instructionCov = rowData.get(2).text();
                            coverageInfo.put("Instruction Coverage", parseCoverage(instructionCov));

                            // 提取分支覆盖率
                            String branchCov = rowData.get(4).text();
                            coverageInfo.put("Branch Coverage", parseCoverage(branchCov));
                            // 提取分支原始数据
                            coverageInfo.put("Branch Missed", rowData.get(3).text());
                            String branchCovText = rowData.get(4).text();
                            if (branchCovText.contains("of")) {
                                String[] parts = branchCovText.split("of");
                                coverageInfo.put("Branch Covered", parts[0].trim());
                                coverageInfo.put("Branch Total", parts[1].trim());
                            }



                            // 提取复杂度
                            coverageInfo.put("Complexity", rowData.get(6).text());

                            // 提取行覆盖率
                            String lineCov = calculateLineCoverage(rowData.get(7).text(), rowData.get(8).text());
                            coverageInfo.put("Line Coverage", lineCov);
                            coverageInfo.put("Line Missed", rowData.get(7).text());
                            coverageInfo.put("Line Total", rowData.get(8).text());

                            // 提取方法覆盖率
                            String methodCov = calculateCoverage(rowData.get(9).text(), rowData.get(10).text());
                            coverageInfo.put("Method Coverage", methodCov);
                            coverageInfo.put("Method Missed", rowData.get(9).text());
                            coverageInfo.put("Method Total", rowData.get(10).text());

                            // 提取类覆盖率
                            String classCov = calculateCoverage(rowData.get(11).text(), rowData.get(12).text());
                            coverageInfo.put("Class Coverage", classCov);
                            coverageInfo.put("Class Missed", rowData.get(11).text());
                            coverageInfo.put("Class Total", rowData.get(12).text());

                            coverageInfoList.add(coverageInfo);
                        }
                    }
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return coverageInfoList;
    }

    private static String parseCoverage(String coverageText) {
        if (coverageText.equalsIgnoreCase("n/a")) {
            return "-";
        } else if (coverageText.contains("%")) {
            return coverageText;
        } else {
            return "0%";
        }
    }

    private static String calculateLineCoverage(String missedLines, String totalLines) {
        if (totalLines.equalsIgnoreCase("0")) {
            return "100%";
        }
        try {
            int missed = Integer.parseInt(missedLines.replace(",", ""));
            int total = Integer.parseInt(totalLines.replace(",", ""));
            double coverage = (double) (total - missed) / total * 100;
            return String.format("%.1f%%", coverage);
        } catch (NumberFormatException e) {
            return "-";
        }
    }

    private static String calculateCoverage(String missed, String total) {
        if (total.equalsIgnoreCase("0")) {
            return "100%";
        }
        try {
            int missedCount = Integer.parseInt(missed.replace(",", ""));
            int totalCount = Integer.parseInt(total.replace(",", ""));
            double coverage = (double) (totalCount - missedCount) / totalCount * 100;
            return String.format("%.1f%%", coverage);
        } catch (NumberFormatException e) {
            return "-";
        }
    }

    public static List<Map<String, String>> filterCoverageInfoByPackage(List<Map<String, String>> coverageInfoList, String targetPackage) {
        List<Map<String, String>> filteredList = new ArrayList<>();
        for (Map<String, String> coverageInfo : coverageInfoList) {
            String element = coverageInfo.get("Element");
            // 标准化包路径格式，替换反斜杠为正斜杠
            String standardizedElement = element.replace("\\", "/");
            
            // 修复：支持匹配包及其所有子包
            // 例如：targetPackage = "cli" 应该匹配 "cli", "cli.help", "cli.help.xxx" 等
            // 检查是否是目标包或其子包
            boolean isMatch = false;
            
            // 情况1：完全匹配目标包（如 "cli"）
            if (standardizedElement.equals(targetPackage)) {
                isMatch = true;
            }
            // 情况2：是目标包的子包（如 "cli/help" 或 "cli.help"）
            else if (standardizedElement.startsWith(targetPackage + "/") || 
                     standardizedElement.startsWith(targetPackage + ".")) {
                isMatch = true;
            }
            // 情况3：包含目标包路径（兼容旧逻辑）
            else if (standardizedElement.contains("/" + targetPackage + "/") ||
                     standardizedElement.contains("/" + targetPackage + ".")) {
                isMatch = true;
            }
            
            if (isMatch) {
                filteredList.add(coverageInfo);
            }
        }
        return filteredList;
    }


    public static void main(String[] args) {
        // 示例用法
        String jacocoReportPath = "target/site/jacoco/index.html"; // 替换为实际的 JaCoCo 报告文件路径
        List<Map<String, String>> coverageInfoList = extractCoverageInfo(jacocoReportPath);
        // 只提取 codeing 包下的结果
        String targetPackage = "codeing";
        List<Map<String, String>> filteredCoverageInfoList = filterCoverageInfoByPackage(coverageInfoList, targetPackage);

        // 输出覆盖率信息
        for (Map<String, String> coverageInfo : filteredCoverageInfoList) {
            String element = coverageInfo.get("Element");
            // 检查是否是方法
            if (element.contains("(")) { // 假设方法名包含括号
                String[] parts = element.split("/");
                if (parts.length >= 2) {
                    String className = parts[0];
                    String methodName = parts[1].split(" ")[0];
                    System.out.println("Class: " + className);
                    System.out.println("  Method: " + methodName);
                } else {
                    System.out.println("Element: " + element);
                }
            } else {
                System.out.println("Element: " + element);
            }
            System.out.println("  Instruction Coverage: " + coverageInfo.get("Instruction Coverage"));
            System.out.println("  Branch Coverage: " + coverageInfo.get("Branch Coverage"));
            System.out.println("  Complexity: " + coverageInfo.get("Complexity"));
            System.out.println("  Line Coverage: " + coverageInfo.get("Line Coverage"));
            System.out.println("  Method Coverage: " + coverageInfo.get("Method Coverage"));
            System.out.println("  Class Coverage: " + coverageInfo.get("Class Coverage"));
            System.out.println("---------------------------");
        }
    }

}
