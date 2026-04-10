package top.hcode.hoj.validator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import top.hcode.hoj.annotation.HOJAccessEnum;
import top.hcode.hoj.common.exception.StatusFailException;
import top.hcode.hoj.exception.AccessException;
import top.hcode.hoj.pojo.dto.TestJudgeDTO;
import top.hcode.hoj.pojo.dto.SubmitJudgeDTO;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * @Author: Himit_ZH
 * @Date: 2022/3/11 11:20
 * @Description:
 */
@Component
public class JudgeValidator {

    @Autowired
    private AccessValidator accessValidator;

    private final static List<String> HOJ_LANGUAGE_LIST = Arrays.asList(
            "C++", "C++ With O2", "C++ 17", "C++ 17 With O2","C++ 20", "C++ 20 With O2",
            "C", "C With O2", "Python3", "Python2", "Java", "Golang", "C#", "PHP", "PyPy2", "PyPy3",
            "JavaScript Node", "JavaScript V8", "Ruby", "Rust");

    private static HashMap<String, String> MODE_MAP_LANGUAGE;

    @PostConstruct
    public void init() {
        MODE_MAP_LANGUAGE = new HashMap<>();
        MODE_MAP_LANGUAGE.put("text/x-c++src", "C++ With O2");
        MODE_MAP_LANGUAGE.put("text/x-csrc", "C With O2");
        MODE_MAP_LANGUAGE.put("text/x-java", "Java");
        MODE_MAP_LANGUAGE.put("text/x-go", "Golang");
        MODE_MAP_LANGUAGE.put("text/x-csharp", "C#");
        MODE_MAP_LANGUAGE.put("text/x-php", "PHP");
        MODE_MAP_LANGUAGE.put("text/x-ruby", "Ruby");
        MODE_MAP_LANGUAGE.put("text/x-rustsrc", "Rust");
    }

    public void validateSubmissionInfo(SubmitJudgeDTO submitJudgeDto) throws StatusFailException, AccessException {

        if (submitJudgeDto.getGid() != null) { // 团队内的提交
            accessValidator.validateAccess(HOJAccessEnum.GROUP_JUDGE);
        } else if (submitJudgeDto.getCid() != null && submitJudgeDto.getCid() != 0) {
            accessValidator.validateAccess(HOJAccessEnum.CONTEST_JUDGE);
        } else {
            accessValidator.validateAccess(HOJAccessEnum.PUBLIC_JUDGE);
        }

        if (!submitJudgeDto.getIsRemote() && !HOJ_LANGUAGE_LIST.contains(submitJudgeDto.getLanguage())) {
            throw new StatusFailException("提交的代码的语言错误！请使用" + HOJ_LANGUAGE_LIST + "中之一的语言！");
        }

        if (submitJudgeDto.getCode().length() < 50
                && !submitJudgeDto.getLanguage().contains("Py")
                && !submitJudgeDto.getLanguage().contains("PHP")
                && !submitJudgeDto.getLanguage().contains("Ruby")
                && !submitJudgeDto.getLanguage().contains("Rust")
                && !submitJudgeDto.getLanguage().contains("JavaScript")) {
            throw new StatusFailException("提交的代码是无效的，代码字符长度请不要低于50！");
        }

        if (submitJudgeDto.getCode().length() > 65535) {
            throw new StatusFailException("提交的代码是无效的，代码字符长度请不要超过65535！");
        }
    }

    public void validateTestJudgeInfo(TestJudgeDTO testJudgeDto) throws StatusFailException, AccessException {
        String type = testJudgeDto.getType();
        switch (type) {
            case "public":
                accessValidator.validateAccess(HOJAccessEnum.PUBLIC_JUDGE);
                break;
            case "contest":
                accessValidator.validateAccess(HOJAccessEnum.CONTEST_JUDGE);
                break;
            case "group":
                accessValidator.validateAccess(HOJAccessEnum.GROUP_JUDGE);
                break;
            default:
                throw new StatusFailException("请求参数type错误！");
        }

        if (StringUtils.isEmpty(testJudgeDto.getCode())) {
            throw new StatusFailException("在线调试的代码不可为空！");
        }

        if (StringUtils.isEmpty(testJudgeDto.getLanguage())) {
            throw new StatusFailException("在线调试的编程语言不可为空！");
        }

        // Remote Judge的编程语言需要转换成HOJ的编程语言才能进行自测
        if (testJudgeDto.getIsRemoteJudge() != null && testJudgeDto.getIsRemoteJudge()) {
            String normalizedLanguage = normalizeRemoteJudgeLanguage(testJudgeDto.getLanguage(), testJudgeDto.getMode());
            if (normalizedLanguage != null) {
                testJudgeDto.setLanguage(normalizedLanguage);
            }
        }

        if (!HOJ_LANGUAGE_LIST.contains(testJudgeDto.getLanguage())) {
            throw new StatusFailException("提交的代码的语言错误！请使用" + HOJ_LANGUAGE_LIST + "中之一的语言！");
        }

        if (StringUtils.isEmpty(testJudgeDto.getUserInput())) {
            throw new StatusFailException("在线调试的输入数据不可为空！");
        }

        if (testJudgeDto.getUserInput().length() > 1000) {
            throw new StatusFailException("在线调试的输入数据字符长度不能超过1000！");
        }

        if (testJudgeDto.getPid() == null) {
            throw new StatusFailException("在线调试所属题目的id不能为空！");
        }

        if (testJudgeDto.getCode().length() < 50
                && !testJudgeDto.getLanguage().contains("Py")
                && !testJudgeDto.getLanguage().contains("PHP")
                && !testJudgeDto.getLanguage().contains("Ruby")
                && !testJudgeDto.getLanguage().contains("Rust")
                && !testJudgeDto.getLanguage().contains("JavaScript")) {
            throw new StatusFailException("提交的代码是无效的，代码字符长度请不要低于50！");
        }

        if (testJudgeDto.getCode().length() > 65535) {
            throw new StatusFailException("提交的代码是无效的，代码字符长度请不要超过65535！");
        }

    }

    private String normalizeRemoteJudgeLanguage(String language, String mode) {
        String modeLanguage = MODE_MAP_LANGUAGE.get(mode);
        if (!StringUtils.isEmpty(modeLanguage)) {
            if ("text/x-c++src".equals(mode)) {
                return normalizeRemoteCppLanguage(language);
            }
            if ("text/x-csrc".equals(mode)) {
                return "C";
            }
            return modeLanguage;
        }

        if (StringUtils.isEmpty(language)) {
            return null;
        }

        String dtoLanguage = language.trim().toLowerCase();

        if (dtoLanguage.contains("pypy 3") || dtoLanguage.contains("pypy3")) {
            return "PyPy3";
        }
        if (dtoLanguage.contains("pypy")) {
            return "PyPy2";
        }
        if (dtoLanguage.contains("python 3")) {
            return "Python3";
        }
        if (dtoLanguage.contains("python")) {
            return "Python2";
        }
        if (dtoLanguage.contains("javascript node") || dtoLanguage.contains("node.js") || dtoLanguage.contains("node")) {
            return "JavaScript Node";
        }
        if (dtoLanguage.contains("javascript") || dtoLanguage.contains("ecmascript") || dtoLanguage.contains("v8")) {
            return "JavaScript V8";
        }
        if (dtoLanguage.contains("c#")) {
            return "C#";
        }
        if (dtoLanguage.contains("rust")) {
            return "Rust";
        }
        if (dtoLanguage.contains("golang") || dtoLanguage.contains("go 1") || dtoLanguage.equals("go")) {
            return "Golang";
        }
        if (dtoLanguage.contains("php")) {
            return "PHP";
        }
        if (dtoLanguage.contains("ruby")) {
            return "Ruby";
        }
        if (dtoLanguage.contains("c++")) {
            return normalizeRemoteCppLanguage(language);
        }
        if ((dtoLanguage.startsWith("c ")
                || dtoLanguage.startsWith("c11")
                || dtoLanguage.startsWith("c17")
                || dtoLanguage.equals("c"))
                && !dtoLanguage.contains("c++")
                && !dtoLanguage.contains("c#")) {
            return "C";
        }
        if (dtoLanguage.contains("java") && !dtoLanguage.contains("javascript") && !dtoLanguage.contains("kotlin")) {
            return "Java";
        }
        return null;
    }

    private String normalizeRemoteCppLanguage(String language) {
        if (StringUtils.isEmpty(language)) {
            return "C++";
        }
        String value = language.toLowerCase();
        if (value.contains("20")) {
            return "C++ 20";
        }
        if (value.contains("17")) {
            return "C++ 17";
        }
        return "C++";
    }
}
