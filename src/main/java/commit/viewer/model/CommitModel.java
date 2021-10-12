package commit.viewer.model;

import com.google.common.base.MoreObjects;

/**
 * Commit Data Model. The model uses builder pattern.
 *
 * @author Sheng Wang (shenggwangg@gmail.com)
 */
public class CommitModel {
    private final String sha;
    private final String message;
    private final String date;

    public String getSha() {
        return sha;
    }

    public String getMessage() {
        return message;
    }

    public String getDate() {
        return date;
    }

    public String getAuthor() {
        return author;
    }

    private final String author;

    private CommitModel(final Builder builder) {
        this.sha = builder.sha;
        this.message = builder.message;
        this.date = builder.date;
        this.author = builder.author;
    }

    public static class Builder {
        private String sha;
        private String message;
        private String date;
        private String author;
        public Builder() {

        }

        public Builder sha(final String val) {
            sha = val;
            return this;
        }
        public Builder message(final String val) {
            message = val;
            return this;
        }
        public Builder date(final String val) {
            date = val;
            return this;
        }
        public Builder author(final String val) {
            author = val;
            return this;
        }
        public CommitModel build() {
            return new CommitModel(this);
        }
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("sha", sha)
                .add("message", message)
                .add("date", date)
                .add("author", author)
                .toString();
    }
}
