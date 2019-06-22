package me.asnxthaony.botapi;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.ErrorHandler;

public class WebErrorHandler extends ErrorHandler {

	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
			throws IOException {
		response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		response.setContentType("text/html;charset=utf-8");
		baseRequest.setHandled(true);

		PrintWriter out = response.getWriter();
		out.print(
				"<!DOCTYPE html><html><head><title>Error</title><style>body{width:35em;margin:0 auto;font-family:Tahoma,Verdana,Arial,sans-serif}</style></head><body><h1>An error occurred.</h1><p>Sorry, the page you are looking for is currently unavailable.<br/>Please try again later.</p><p>If you are the system administrator of this resource then you should checkthe error log for details.</p><p><em>Faithfully yours, jetty.</em></p></body></html>");
	}

}
