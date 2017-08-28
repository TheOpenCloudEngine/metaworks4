package org.metaworks.iam;

import com.nimbusds.jwt.JWTClaimsSet;
import org.apache.commons.io.*;
import org.metaworks.annotation.Default;
import org.metaworks.common.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by uengine on 2016. 4. 22..
 */
@WebFilter
public class IamRestFilter implements Filter {

    @Value("${iam.clientKey}")
    @Default("e74a9505-a811-407f-b4f6-129b7af1c703")
    private String clientKey;

    @Value("${iam.clientSecretKey}")
    @Default("109cf590-ac67-4b8c-912a-913373ada046")
    private String clientSecretKey;

    @Value("${iam.host}")
    @Default("http://iam.uengine.io:8080")
    private String iamHost;

    @Value("${iam.applicationRestEndPoint}")
    @Default("/iam")
    private String applicationRestEndPoint;

    @Value("${iam.jwtLocalValidate}")
    @Default("true")
    private boolean jwtLocalValidate;

    @Value("${iam.jwtIssuer}")
    @Default("oce.iam")
    private String jwtIssuer;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        HttpServletResponse response = (HttpServletResponse) res;
        HttpServletRequest request = (HttpServletRequest) req;
        String requestURI = request.getRequestURI();

        if (requestURI.startsWith(this.applicationRestEndPoint)) {
            try {
                //토큰 밸리데이션 요청에 한하여, jwtLocalValidate 이면서, OPTIONS 가 아니고, JWT 토큰인 경우 로컬에서 밸리데이션 수행.
                if (requestURI.endsWith("/token_info") && this.jwtLocalValidate && !request.getMethod().equals("OPTIONS")) {
                    String token = req.getParameter("access_token");
                    boolean matches = token.matches("[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}");
                    if (!matches) {

                        Map map = new HashMap();
                        JWTClaimsSet jwtClaimsSet = null;
                        try {
                            jwtClaimsSet = JwtUtils.parseToken(token);
                        } catch (Exception ex) {
                            this.responseError(OauthConstant.INVALID_TOKEN, "Invalid jwt token.", response);
                            return;
                        }

                        //이슈어 확인
                        String issuer = jwtClaimsSet.getIssuer();
                        if (!this.jwtIssuer.equals(issuer)) {
                            this.responseError(OauthConstant.INVALID_TOKEN, "Invalid issuer.", response);
                            return;
                        }

                        boolean validated = JwtUtils.validateToken(token);
                        if (!validated) {
                            this.responseError(OauthConstant.INVALID_TOKEN, "Invalid token secret.", response);
                            return;
                        }

                        //코드의 발급시간을 확인한다.
                        Date currentTime = new Date();
                        Date expirationTime = jwtClaimsSet.getExpirationTime();
                        long diff = (long) Math.floor((expirationTime.getTime() - currentTime.getTime()) / 1000);

                        if (diff <= 0) {
                            this.responseError(OauthConstant.INVALID_TOKEN, "requested access_token has expired.", response);
                            return;
                        } else {
                            map.put("expires_in", diff);
                        }

                        Map<String, Object> claims = jwtClaimsSet.getClaims();
                        map.putAll(claims);

                        Map userData = (Map) ((Map) map.get("context")).get("user");

                        //Provide spring security context
//                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userData, null, null);
//                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
//                        SecurityContextHolder.getContext().setAuthentication(authentication);

                        String marshal = JsonUtils.marshal(map);
                        String prettyPrint = JsonFormatterUtils.prettyPrint(marshal);


                        response.setHeader("Content-Type", "application/json;charset=UTF-8");
                        response.setHeader("Cache-Control", "no-store");
                        response.setHeader("Pragma", "no-cache");
                        response.setStatus(200);
                        this.addIAMCors(response);
                        response.getWriter().write(prettyPrint);
                        return;
                    }
                }

                doProxy(request, response);
                return;
            } catch (Exception ex) {
                response.setStatus(400);
                this.addIAMCors(response);
                ExceptionUtils.httpExceptionResponse(ex, response);
                return;
            }
        } else {
            response.setHeader("Access-Control-Allow-Origin", "*");
            response.setHeader("Access-Control-Allow-Methods", "POST, GET, DELETE, PUT, OPTIONS");
            response.setHeader("Access-Control-Max-Age", "3600");
            response.setHeader("Access-Control-Allow-Headers", "x-requested-with, origin, content-type, accept, " +
                    "management-key, management-secret, client-key, client-secret, authorization, Location, access_token");
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

    private void responseError(String error, String error_description, HttpServletResponse response) {
        try {
            Map map = new HashMap();
            map.put("error", error);
            map.put("error_description", error_description);

            String marshal = JsonUtils.marshal(map);
            String prettyPrint = JsonFormatterUtils.prettyPrint(marshal);

            response.setHeader("Content-Type", "application/json;charset=UTF-8");
            response.setHeader("Cache-Control", "no-store");
            response.setHeader("Pragma", "no-cache");
            response.setStatus(400);
            this.addIAMCors(response);
            response.getWriter().write(prettyPrint);
        } catch (IOException ex) {
            //response 전달 과정 중 실패가 일어나더라도 프로세스에는 영향을 끼지지 않는다.
            response.setStatus(400);
            this.addIAMCors(response);
            ExceptionUtils.httpExceptionResponse(ex, response);
        }
    }

    private void addIAMCors(HttpServletResponse response) {
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "POST, GET, DELETE, PUT");
        response.setHeader("Access-Control-Max-Age", "3600");
        response.setHeader("Access-Control-Allow-Headers", "x-requested-with, origin, content-type, accept, " +
                "management-key, management-secret, client-key, client-secret, authorization, Location, access_token");
    }
}
