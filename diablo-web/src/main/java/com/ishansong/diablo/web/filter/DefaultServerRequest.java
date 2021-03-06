package com.ishansong.diablo.web.filter;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.multipart.Part;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyExtractor;
import org.springframework.web.reactive.function.BodyExtractors;
import org.springframework.web.reactive.function.UnsupportedMediaTypeException;
import org.springframework.web.reactive.function.server.HandlerStrategies;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.UnsupportedMediaTypeStatusException;
import org.springframework.web.server.WebSession;
import org.springframework.web.util.UriBuilder;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.Charset;
import java.security.Principal;
import java.util.*;
import java.util.function.Function;

public class DefaultServerRequest implements ServerRequest {

    private final ServerWebExchange exchange;
    private final Headers headers;
    private final List<HttpMessageReader<?>> messageReaders;

    private final Function<UnsupportedMediaTypeException, UnsupportedMediaTypeStatusException> ERROR_MAPPER =
            f -> f.getContentType() != null ? new UnsupportedMediaTypeStatusException(f.getContentType(), f.getSupportedMediaTypes()) : new UnsupportedMediaTypeStatusException(f.getMessage());

    public DefaultServerRequest(ServerWebExchange exchange) {
        this(exchange, HandlerStrategies.withDefaults().messageReaders());
    }

    public DefaultServerRequest(ServerWebExchange exchange, List<HttpMessageReader<?>> messageReaders) {
        this.exchange = exchange;
        this.messageReaders = messageReaders;
        this.headers = new DefaultHeaders();
    }

    @Override
    public String methodName() {

        return request().getMethodValue();
    }

    @Override
    public URI uri() {

        return request().getURI();
    }

    @Override
    public UriBuilder uriBuilder() {

        return UriComponentsBuilder.fromHttpRequest(new ServerRequestAdapter());
    }

    @Override
    public Headers headers() {

        return this.headers;
    }

    @Override
    public MultiValueMap<String, HttpCookie> cookies() {

        return request().getCookies();
    }

    @Override
    public Optional<InetSocketAddress> remoteAddress() {

        return Optional.ofNullable(request().getRemoteAddress());
    }

    @Override
    public List<HttpMessageReader<?>> messageReaders() {

        return this.messageReaders;
    }

    @Override
    public <T> T body(BodyExtractor<T, ? super ServerHttpRequest> extractor, Map<String, Object> hints) {

        return extractor.extract(request(), new BodyExtractor.Context() {

            @Override
            public List<HttpMessageReader<?>> messageReaders() {
                return messageReaders;
            }

            @Override
            public Optional<ServerHttpResponse> serverResponse() {
                return Optional.ofNullable(exchange()).map(ServerWebExchange::getResponse);
            }

            @Override
            public Map<String, Object> hints() {
                return hints;
            }
        });
    }

    @Override
    public <T> T body(BodyExtractor<T, ? super ServerHttpRequest> extractor) {

        return body(extractor, Collections.emptyMap());
    }

    @Override
    public <T> Mono<T> bodyToMono(Class<? extends T> elementClass) {

        Mono<T> mono = body(BodyExtractors.toMono(elementClass));

        return mono.onErrorMap(UnsupportedMediaTypeException.class, ERROR_MAPPER);
    }

    @Override
    public <T> Mono<T> bodyToMono(ParameterizedTypeReference<T> typeReference) {

        Mono<T> mono = body(BodyExtractors.toMono(typeReference));

        return mono.onErrorMap(UnsupportedMediaTypeException.class, ERROR_MAPPER);
    }

    @Override
    public <T> Flux<T> bodyToFlux(Class<? extends T> elementClass) {

        Flux<T> flux = body(BodyExtractors.toFlux(elementClass));

        return flux.onErrorMap(UnsupportedMediaTypeException.class, ERROR_MAPPER);
    }

    @Override
    public <T> Flux<T> bodyToFlux(ParameterizedTypeReference<T> typeReference) {

        Flux<T> flux = body(BodyExtractors.toFlux(typeReference));

        return flux.onErrorMap(UnsupportedMediaTypeException.class, ERROR_MAPPER);
    }

    @Override
    public Map<String, Object> attributes() {

        return this.exchange.getAttributes();
    }

    @Override
    public MultiValueMap<String, String> queryParams() {

        return request().getQueryParams();
    }

    @Override
    public Map<String, String> pathVariables() {

        return this.exchange.getAttributeOrDefault(RouterFunctions.URI_TEMPLATE_VARIABLES_ATTRIBUTE, Collections.emptyMap());
    }

    @Override
    public Mono<WebSession> session() {

        return this.exchange.getSession();
    }

    @Override
    public Mono<? extends Principal> principal() {

        return this.exchange.getPrincipal();
    }

    @Override
    public Mono<MultiValueMap<String, String>> formData() {

        return this.exchange.getFormData();
    }

    @Override
    public Mono<MultiValueMap<String, Part>> multipartData() {

        return this.exchange.getMultipartData();
    }

    @Override
    public ServerWebExchange exchange() {

        return this.exchange;
    }

    private ServerHttpRequest request() {
        return this.exchange.getRequest();
    }

    private class DefaultHeaders implements Headers {

        @Override
        public List<MediaType> accept() {
            return delegate().getAccept();
        }

        @Override
        public List<Charset> acceptCharset() {
            return delegate().getAcceptCharset();
        }

        @Override
        public List<Locale.LanguageRange> acceptLanguage() {
            return delegate().getAcceptLanguage();
        }

        @Override
        public OptionalLong contentLength() {
            long contentLength = delegate().getContentLength();

            return contentLength != -1 ? OptionalLong.of(contentLength) : OptionalLong.empty();
        }

        @Override
        public Optional<MediaType> contentType() {
            return Optional.ofNullable(delegate().getContentType());
        }

        @Override
        public InetSocketAddress host() {
            return delegate().getHost();
        }

        @Override
        public List<HttpRange> range() {
            return delegate().getRange();
        }

        @Override
        public List<String> header(String headerName) {
            List<String> headerValues = delegate().get(headerName);

            return headerValues != null ? headerValues : Collections.emptyList();
        }

        @Override
        public HttpHeaders asHttpHeaders() {
            return HttpHeaders.readOnlyHttpHeaders(delegate());
        }

        @Override
        public String toString() {
            return delegate().toString();
        }

        private HttpHeaders delegate() {

            return request().getHeaders();
        }
    }

    private final class ServerRequestAdapter implements HttpRequest {

        @Override
        public String getMethodValue() {
            return methodName();
        }

        @Override
        public URI getURI() {
            return uri();
        }

        @Override
        public HttpHeaders getHeaders() {
            return request().getHeaders();
        }
    }
}
