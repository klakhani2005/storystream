package com.portfolio.storystream.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CatalogServiceTest {

    @Test
    void likePatternLowercasesTrimsAndWraps() {
        assertThat(CatalogService.likePattern("  The MARTIAN ")).isEqualTo("%the martian%");
    }

    @Test
    void likePatternEscapesWildcardsSoUserInputIsLiteral() {
        assertThat(CatalogService.likePattern("100%_done\\")).isEqualTo("%100\\%\\_done\\\\%");
    }
}
