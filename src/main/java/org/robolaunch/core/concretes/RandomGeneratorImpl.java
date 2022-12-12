package org.robolaunch.core.concretes;

import java.security.SecureRandom;

import javax.enterprise.context.ApplicationScoped;

import org.robolaunch.core.abstracts.RandomGenerator;

@ApplicationScoped
public class RandomGeneratorImpl implements RandomGenerator {

    @Override
    public String generateRandomString(int size) {
        // ASCII range – alphanumeric (0-9, a-z, A-Z)
        final String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder();

        // each iteration of the loop randomly chooses a character from the given
        // ASCII range and appends it to the `StringBuilder` instance

        for (int i = 0; i < size; i++) {
            int randomIndex = random.nextInt(chars.length());
            sb.append(chars.charAt(randomIndex));
        }

        return sb.toString();
    }

}
