package org.metaworks.multitenancy.tenantawarefilter;

/**
 * Created by uengine on 2017. 11. 14..
 */
public class TokenContext {
    static ThreadLocal<TokenContext> local = new ThreadLocal();
    String token;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public TokenContext(String token) {
        this.token = token;
        local.set(this);
    }

    public static TokenContext getThreadLocalInstance() {
        TokenContext tc = (TokenContext) local.get();
        return tc != null ? tc : new TokenContext((String) null);
    }
}
