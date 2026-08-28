package veterinaria.vargasvet.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class PaginationParameterFilterTest {

    private final PaginationParameterFilter filter = new PaginationParameterFilter(100);

    @Test
    void acceptsValidPagination() throws Exception {
        MockHttpServletRequest request = request("2", "50");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void rejectsOversizedPage() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request("0", "101"), response, chain);

        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getContentAsString()).contains("entre 1 y 100");
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void rejectsNonNumericParameters() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request("invalid", "20"), response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getContentAsString()).contains("número entero");
    }

    private MockHttpServletRequest request(String page, String size) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/pets");
        request.addParameter("page", page);
        request.addParameter("size", size);
        return request;
    }
}
