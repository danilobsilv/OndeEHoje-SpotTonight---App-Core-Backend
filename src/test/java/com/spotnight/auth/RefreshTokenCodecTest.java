package com.spotnight.auth;

import static org.assertj.core.api.Assertions.*;
import com.spotnight.auth.service.RefreshTokenCodec;
import org.junit.jupiter.api.Test;

class RefreshTokenCodecTest {
    private final RefreshTokenCodec codec = new RefreshTokenCodec();

    @Test
    void tokensAreRandomUrlSafeAndHashesAreDeterministicSha256() {
        String first = codec.generate();
        assertThat(first).hasSize(43).matches("[A-Za-z0-9_-]+");
        assertThat(codec.generate()).isNotEqualTo(first);
        assertThat(codec.hash(first)).hasSize(64).isEqualTo(codec.hash(first)).isNotEqualTo(first);
        assertThat(codec.hash("abc")).isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
        assertThat(codec.validFormat(null)).isFalse();
        assertThat(codec.validFormat("bad")).isFalse();
    }
}
