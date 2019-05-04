package me.asnxthaony.botapi;

public class Response {

	private int code;
	private String message;

	public Response(int code, String message) {
		setCode(code);
		setMessage(message);
	}

	public int getCode() {
		return code;
	}

	public String getMessage() {
		return message;
	}

	public void setCode(int code) {
		this.code = code;
	}

	public void setMessage(String message) {
		this.message = message;
	}

}
