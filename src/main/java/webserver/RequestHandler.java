package webserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import webserver.http.*;
import webserver.http.controller.*;
import webserver.http.dispatcher.DefaultHttpRequestDispatcher;
import webserver.http.dispatcher.HttpRequestDispatcher;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;

public class RequestHandler implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(RequestHandler.class);

    private Socket connection;

    private final static Controller STATIC_RESOURCE_CONTROLLER = new StaticResourceController("./static");

    private final static HttpRequestDispatcher dispatcher = new DefaultHttpRequestDispatcher(
            new RegexpMapping("\\/css\\/.+", HttpMethod.GET, STATIC_RESOURCE_CONTROLLER),
            new RegexpMapping("\\/js\\/.+", HttpMethod.GET, STATIC_RESOURCE_CONTROLLER),
            new RegexpMapping("\\/fonts\\/.+", HttpMethod.GET, STATIC_RESOURCE_CONTROLLER),
            new RegexpMapping("\\/.+\\.html", HttpMethod.GET, new HtmlController()),
            new RegexpMapping("\\/user\\/create", HttpMethod.POST, new SignUpController()),
            new RegexpMapping("\\/user\\/login", HttpMethod.POST, new LoginController()),
            new RegexpMapping("\\/user\\/list", HttpMethod.GET, new UserListController())
    );

    public RequestHandler(Socket connectionSocket) {
        this.connection = connectionSocket;
    }

    @Override
    public void run() {
        logger.debug("New Client Connect! Connected IP : {}, Port : {}", connection.getInetAddress(),
                connection.getPort());
        try {
            InputStream in = connection.getInputStream();
            OutputStream out = connection.getOutputStream();

            HttpRequest httpRequest = createHttpRequest(in);
            printAllRequestHeaders(httpRequest);

            HttpResponse httpResponse = new HttpResponse(out);
            dispatcher.dispatch(httpRequest, httpResponse);

            printAllResponseHeaders(httpResponse);
            httpResponse.send();

        } catch (Exception e) {
            e.printStackTrace();
            logger.error(e.getMessage());
        } finally {
            closeConnection();
        }
    }

    private void closeConnection() {
        try {
            connection.close();
        } catch (IOException e) {
            throw new RuntimeException("socket close 과정에 문제가 발생했습니다", e);
        }
    }

    private HttpRequest createHttpRequest(InputStream in) {
        return new HttpRequestFactory().create(new InputStreamReader(in));
    }

    private void printAllRequestHeaders(HttpRequest httpRequest) {
        logger.debug("---- request-line ----");
        logger.debug(httpRequest.getRequestLine());
        logger.debug("---- request-header ----");
        httpRequest.getHeaders().forEach(it -> logger.debug(it.toString()));
        logger.debug("---- reqeust-body ----");
        logger.debug(httpRequest.getBody());
    }

    private void printAllResponseHeaders(HttpResponse httpResponse) {
        logger.debug("---- response-status-line ---");
        logger.debug(httpResponse.getStatusLine());
        logger.debug("---- response-header ----");
        httpResponse.getHeaders().forEach(it -> logger.debug(it.toString()));
    }


}
