package org.lambda.framework.common.po;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScopedLoginUser implements SecurityLoginUser<String> {
    private String id;
    private Realm realm;
    private Terminal terminal;

    public enum Realm {
        APP, ADMIN
    }

    public enum Terminal {
        MINI_PROGRAM("mp-weixin"),
        IOS("ios"),
        ANDROID("android"),
        CONSOLE("console");

        private final String code;

        Terminal(String code) {
            this.code = code;
        }

        public String code() {
            return code;
        }

        public boolean supports(Realm realm) {
            return realm != null && (realm == Realm.ADMIN) == (this == CONSOLE);
        }
    }
}
