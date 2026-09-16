package com.telemisl.rcher.modules.telemisbowling.session;

import java.util.UUID;

public record PlayerToRegister(String displayName, UUID sessionToken) {
}
