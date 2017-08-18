package org.metaworks.iam;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by uengine on 2016. 4. 22..
 */
@WebFilter
public class IamRestFilter implements Filter {

    private String clientKey;
    private String clientSecretKey;
    private String iamHost;
    private String applicationRestEndPoint;

    public String getClientKey() {
        return clientKey;
    }

    public void setClientKey(String clientKey) {
        this.clientKey = clientKey;
    }

    public String getClientSecretKey() {
        return clientSecretKey;
    }

    public void setClientSecretKey(String clientSecretKey) {
        this.clientSecretKey = clientSecretKey;
    }

    public String getIamHost() {
        return iamHost;
    }

    public void setIamHost(String iamHost) {
        this.iamHost = iamHost;
    }

    public String getApplicationRestEndPoint() {
        return applicationRestEndPoint;
    }

    public void setApplicationRestEndPoint(String applicationRestEndPoint) {
        this.applicationRestEndPoint = applicationRestEndPoint;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        HttpServletResponse response = (HttpServletResponse) res;
        HttpServletRequest request = (HttpServletRequest) req;
        String requestURI = request.getRequestURI();
        if(requestURI.startsWith(this.applicationRestEndPoint)){
            try {
                doProxy(request, response);
                return;
            } catch (Exception ex) {
                ex.printStackTrace();
                response.setStatus(500);
                return;
            }
        } else {
            chain.doFilter(req, res);
        }
    }

    @Override
    public void destroy() {

    }

    private void doProxy(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Map requiredHeaders = new HashMap();
        requiredHeaders.put("client-key", this.clientKey);
        requiredHeaders.put("client-secret", this.clientSecretKey);

        ProxyRequest proxyRequest = new ProxyRequest();
        proxyRequest.setRequest(request);
        proxyRequest.setResponse(response);

        proxyRequest.setHost(this.iamHost);
        proxyRequest.setPath(request.getRequestURI().replace(this.applicationRestEndPoint, ""));
        proxyRequest.setHeaders(requiredHeaders);

        new ProxyService().doProxy(proxyRequest);
    }

}
