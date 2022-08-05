package filters;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@javax.servlet.annotation.WebFilter(filterName = "WebFilter", urlPatterns = {"/*"})

public class WebFilter implements Filter {

    public static String VALID_METHODS = "DELETE, HEAD, GET, OPTIONS, POST, PUT";

    @Override
    public void destroy() {}

    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) resp;

        //String servletPath = request.getServletPath();

        if (request.getServletPath().equals("/v1/api")){

            String ip = getClientIpAddress(request);
            if (!ip.startsWith("10.108")){
                request.setAttribute("badIP", Boolean.TRUE);
            }else{
                request.setAttribute("badIP", Boolean.FALSE);
            }
        }

        String origin = request.getHeader("Origin");
        if (origin == null) {

            if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
                response.setHeader("Allow", VALID_METHODS);
                response.setStatus(200);
                return;
            }
        } else {
            response.setHeader("Access-Control-Allow-Origin", origin);
            response.setHeader("Access-Control-Allow-Methods", VALID_METHODS);

            String headers = request.getHeader("Access-Control-Request-Headers");
            if (headers != null)
                response.setHeader("Access-Control-Allow-Headers", headers);

            response.setHeader("Access-Control-Max-Age", "3600");
        }
        if (!"OPTIONS".equalsIgnoreCase(request.getMethod())) {
            chain.doFilter(req, resp);
        }

        //chain.doFilter(request, response);
    }

    public void init(FilterConfig config) {}

    private String getClientIpAddress(HttpServletRequest request) {
        /*for (String header : HEADERS_TO_TRY) {
            String ip = request.getHeader(header);
            if (ip != null && ip.length() != 0 && !"unknown".equalsIgnoreCase(ip)) {
                return ip;
            }
        }*/
        return request.getRemoteAddr();
    }
}
