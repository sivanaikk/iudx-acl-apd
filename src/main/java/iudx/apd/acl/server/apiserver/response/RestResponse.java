package iudx.apd.acl.server.apiserver.response;

import io.vertx.core.json.JsonObject;

public class RestResponse {
    private final String type;
    private final String title;
    private final String detail;

    private RestResponse(String type, String title, String detail)
    {
        super();
        this.type = type;
        this.title = title;
        this.detail = detail;
    }

    public JsonObject toJson(){
        return new JsonObject()
                .put("type",this.type)
                .put("title",this.title)
                .put("detail",this.detail);
    }


    public static class Builder {
        private String type;
        private String title;
        private String detail;

        public Builder(){
        }

        public Builder withType(String type)
        {
            this.type = type;
            return this;
        }

        public Builder withTitle(String title)
        {
            this.title = title;
            return this;
        }

        public Builder withMessage(String message)
        {
            this.detail = message;
            return this;
        }

        public RestResponse build(){
            return new RestResponse(this.type,this.title,this.detail);
        }
    }
}
