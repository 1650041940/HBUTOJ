package top.hcode.hoj.crawler.problem;

import cn.hutool.core.util.ReUtil;
import cn.hutool.http.HttpUtil;
import org.jsoup.Jsoup;
import top.hcode.hoj.pojo.entity.problem.Problem;
import top.hcode.hoj.utils.Constants;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Author: Himit_ZH
 * @Date: 2022/1/28 21:23
 * @Description:
 */
public class AtCoderProblemStrategy extends ProblemStrategy {

    public static final String JUDGE_NAME = "AC";
    public static final String HOST = "https://atcoder.jp";
    public static final String PROBLEM_URL = "/contests/%s/tasks/%s";

    public String getJudgeName() {
        return JUDGE_NAME;
    }

    public String getProblemUrl(String problemId, String contestId) {
        return HOST + String.format(PROBLEM_URL, contestId, problemId);
    }

    public String getProblemSource(String problemId, String contestId) {
        return String.format("<a style='color:#1A5CC8' href='" + getProblemUrl(problemId, contestId) + "'>%s</a>", "AtCoder-" + problemId);
    }

    @Override
    public RemoteProblemInfo getProblemInfo(String problemId, String author) throws Exception {

        problemId = problemId == null ? null : problemId.trim().toLowerCase();
        boolean isMatch = ReUtil.isMatch("^[a-z0-9]+_[a-z0-9]+$", problemId);
        if (!isMatch) {
            throw new IllegalArgumentException("AtCoder: 题号格式不正确，必须类似 abc110_a（仅包含小写字母/数字与下划线）");
        }

        String contestId = problemId.split("_")[0];

        String url = getProblemUrl(problemId, contestId);
        String body = HttpUtil.get(url);

        Pattern pattern = Pattern.compile(
            "Time\\s*Limit:\\s*([0-9]+(?:\\.[0-9]+)?)\\s*sec\\s*/\\s*Memory\\s*Limit:\\s*(\\d+)\\s*M(?:i)?B",
                Pattern.CASE_INSENSITIVE
        );
        Matcher matcher = pattern.matcher(body);
        if (!matcher.find()) {
            throw new IllegalArgumentException(
                    "AtCoder: 获取题目信息失败（无法解析时限/内存）。" +
                            "请确认题号存在且服务器可访问：" + url +
                            "；若服务器网络受限/被拦截，也会导致解析失败。"
            );
        }

        double timeLimitSec = Double.parseDouble(matcher.group(1).trim());
        int timeLimitMs = (int) Math.round(timeLimitSec * 1000.0);
        int memoryLimitMb = Integer.parseInt(matcher.group(2).trim());

        String title = ReUtil.get("<title>[\\s\\S]*? - ([\\s\\S]*?)</title>", body, 1);
        if (title == null) {
            String rawTitle = Jsoup.parse(body).title();
            if (rawTitle != null && rawTitle.contains(" - ")) {
                title = rawTitle.substring(rawTitle.lastIndexOf(" - ") + 3);
            } else {
                title = rawTitle;
            }
        }


        Problem problem = new Problem();
        problem.setProblemId(getJudgeName() + "-" + problemId)
                .setAuthor(author)
                .setTitle(title)
                .setType(0)
            .setTimeLimit(timeLimitMs)
            .setMemoryLimit(memoryLimitMb)
                .setIsRemote(true)
                .setSource(getProblemSource(problemId, contestId))
                .setAuth(1)
                .setOpenCaseResult(false)
                .setIsRemoveEndBlank(false)
                .setIsGroup(false)
                .setDifficulty(1); // 默认为中等

        if (body.contains("Problem Statement")) {
            String desc = ReUtil.get("<h3>Problem Statement</h3>([\\s\\S]*?)</section>[\\s\\S]*?</div>", body, 1);

            desc = desc.replaceAll("<var>", "\\$").replaceAll("</var>", "\\$");
            desc = desc.replaceAll("<pre>", "<pre style=\"padding:9px!important;background-color: #f5f5f5!important\">");
            desc = desc.replaceAll("src=\"/img", "src=\"" + HOST + "/img");

            StringBuilder sb = new StringBuilder();
            String rawInput = ReUtil.get("<h3>Input</h3>([\\s\\S]*?)</section>[\\s\\S]*?</div>", body, 1);
            sb.append(rawInput);
            String constrains = ReUtil.get("<h3>Constraints</h3>([\\s\\S]*?)</section>[\\s\\S]*?</div>", body, 1);
            sb.append(constrains);
            String input = sb.toString().replaceAll("<var>", "\\$").replaceAll("</var>", "\\$");
            input = input.replaceAll("<pre>", "<pre style=\"padding:9px!important;background-color: #f5f5f5!important\">");


            String rawOutput = ReUtil.get("<h3>Output</h3>([\\s\\S]*?)</section>[\\s\\S]*?</div>", body, 1);
            String output = rawOutput.replaceAll("<var>", "\\$").replaceAll("</var>", "\\$");
            output = output.replaceAll("<pre>", "<pre style=\"padding:9px!important;background-color: #f5f5f5!important\">");

            List<String> sampleInput = ReUtil.findAll("<h3>Sample Input \\d+</h3><pre>([\\s\\S]*?)</pre>[\\s\\S]*?</section>[\\s\\S]*?</div>", body, 1);
            List<String> sampleOutput = ReUtil.findAll("<h3>Sample Output \\d+</h3><pre>([\\s\\S]*?)</pre>[\\s\\S]*?</section>[\\s\\S]*?</div>", body, 1);


            StringBuilder examples = new StringBuilder();

            for (int i = 0; i < sampleInput.size() && i < sampleOutput.size(); i++) {
                examples.append("<input>");
                String exampleInput = sampleInput.get(i).trim();
                examples.append(exampleInput).append("</input>");
                examples.append("<output>");
                String exampleOutput = sampleOutput.get(i).trim();
                examples.append(exampleOutput).append("</output>");
            }

            problem.setInput(input.trim())
                    .setOutput(output.trim())
                    .setDescription(desc.trim())
                    .setExamples(examples.toString());


        } else {
            org.jsoup.nodes.Element element = Jsoup.parse(body).getElementById("task-statement");
            String desc = element.html();
            desc = desc.replaceAll("src=\"/img", "src=\"https://atcoder.jp/img");
            desc = desc.replaceAll("<pre>", "<pre style=\"padding:9px!important;background-color: #f5f5f5!important\">");
            desc = desc.replaceAll("<var>", "\\$").replaceAll("</var>", "\\$");
            desc = desc.replaceAll("<hr>", "");
            problem.setDescription(desc);
        }
        return new RemoteProblemInfo()
                .setProblem(problem)
                .setTagList(null)
                .setLangIdList(null)
                .setRemoteOJ(Constants.RemoteOJ.ATCODER);
    }
}