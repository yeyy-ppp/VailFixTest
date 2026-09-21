package zju.cst.aces.dto;

import lombok.Data;

import java.util.List;

@Data
public class RoundRecord {
    public int attempt;
    public int round;
    public List<ChatMessage> prompt;
    public String response;
    public int promptToken;
    public int responseToken;
    public boolean hasCode;
    public String code;
    public boolean hasError;
    public TestMessage errorMsg;

    private String validationResponse; // 用于存储验证响应

    public RoundRecord(int round) {
        this.round = round;
    }

    public String getValidationResponse() {
        return validationResponse;
    }

    public void setValidationResponse(String validationResponse) {
        this.validationResponse = validationResponse;
    }
}
