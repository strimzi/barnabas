/*
 * Copyright 2018, Strimzi Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.strimzi.controller.topic;

import io.fabric8.kubernetes.api.model.ConfigMap;
import org.apache.kafka.common.internals.Topic;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Typesafe representation of the name of a topic.
 */
class TopicName {
    private final String name;

    public TopicName(String name) {
        assert(name != null && !name.isEmpty());
        // TODO Shame we can't validate a topic name without relying on an internal class
        Topic.validate(name);
        this.name = name;
    }

    public TopicName(ConfigMap cm) {
        this(cm.getData().getOrDefault("name", cm.getMetadata().getName()));
    }

    public String toString() {
        return this.name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TopicName topicName = (TopicName) o;

        return name.equals(topicName.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    // This is the same regex used by kubernetes (or at least oc create)
    public static final String SEP = "---";

    /**
     * Return a valid map name for the given topic name. If the topic name is already valid as a resource name
     * then it is used as the returned resource name, otherwise a "best effort" prefix is
     * constructed (with invalid characters removed or changed) and a disambiguating hash is appended to that
     * prefix and the concatenation of the prefix and hash is returned.
     */
    public MapName asMapName() {
        MapName mname;
        if (MapName.isValidResourceName(this.name)) {
            mname = new MapName(this.name);
        } else {
            StringBuilder n = new StringBuilder();
            for (int i = 0; i < this.name.length(); i++) {
                char next = i < this.name.length() - 1 ? this.name.charAt(i+1) : '\0';
                char ch = this.name.charAt(i);
                if (isInRange('a', ch,'z')
                        || isInRange('0', ch, '9')) {
                    n.append(ch);
                } else if (isInRange('A', ch, 'Z')) {
                    n.append(Character.toLowerCase(ch));
                } else if (ch == '-' || ch == '.' || ch == '_') {
                    // avoid hyphen next to dot in the output
                    for (int j = n.length()-1; j >= 0; j--) {
                        if (isInRange('a', n.charAt(j), 'z')
                                || isInRange('0', n.charAt(j), '9')) {
                            n.append(ch == '_' ? '-' : ch);
                            break;
                        } else {
                            break;
                        }
                    }
                }
                // only other possibiilty for ch is underscore, which is always invalid
            }

            // it's still possible that n ends with a sequence of hyphens or dots
            int cut = 0;
            for (int j = n.length()-1; j >= 0; j--) {
                char ch = n.charAt(j);
                if (ch == '.' || ch == '-') {
                    cut++;
                } else {
                    break;
                }
            }
            n.setLength(n.length()-cut);

            final MessageDigest md;
            try {
                md = MessageDigest.getInstance("SHA-1");
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException("Couldn't get SHA1 MessageDigest", e);
            }
            final int sha1HexLength = 40;
            byte[] sha1sum = md.digest(this.name.getBytes(StandardCharsets.UTF_8));
            int truncate = n.length() + sha1HexLength + SEP.length() - MapName.MAX_RESOURCE_NAME_LENGTH;
            if (truncate > 0) {
                n.setLength(MapName.MAX_RESOURCE_NAME_LENGTH - (sha1HexLength + SEP.length()));
            }
            // It's still possible that n is empty by this point
            // (if tname consisted entirely of chars at invalid positions)
            // and starting the name with "---" would make it invalid, so
            // only add the SEP if there's something to separate.
            if (n.length() > 0) {
                n.append(SEP);
            }
            n.append(new BigInteger(1, sha1sum).toString(16));
            mname = new MapName(n.toString());
        }
        return mname;
    }

    private boolean isInRange(char a, char ch, char z) {
        return a <= ch && ch <= z;
    }
}
