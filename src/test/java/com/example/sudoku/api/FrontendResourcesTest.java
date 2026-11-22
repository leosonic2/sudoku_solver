package com.example.sudoku.api;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class FrontendResourcesTest {
    @Autowired
    MockMvc mvc;

    @Test
    void servesVueBrowserModuleFromWebJar() throws Exception {
        mvc.perform(get("/webjars/vue/3.5.46/dist/vue.esm-browser.prod.js"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type",
                        anyOf(startsWith("application/javascript"), startsWith("text/javascript"))))
                .andExpect(content().string(containsString("export")));
    }
}
