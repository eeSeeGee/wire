// Code generated by Wire protocol buffer compiler, do not edit.
// Source file: letter.proto
package squareup.options.letter;

import com.squareup.wire.FieldEncoding;
import com.squareup.wire.Message;
import com.squareup.wire.ProtoAdapter;
import com.squareup.wire.ProtoReader;
import com.squareup.wire.ProtoWriter;
import java.io.IOException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.StringBuilder;
import okio.ByteString;

public final class Letter extends Message<Letter, Letter.Builder> {
  public static final ProtoAdapter<Letter> ADAPTER = new ProtoAdapter_Letter();

  private static final long serialVersionUID = 0L;

  public Letter() {
    this(ByteString.EMPTY);
  }

  public Letter(ByteString unknownFields) {
    super(ADAPTER, unknownFields);
  }

  @Override
  public Builder newBuilder() {
    Builder builder = new Builder();
    builder.addUnknownFields(unknownFields());
    return builder;
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) return true;
    if (!(other instanceof Letter)) return false;
    Letter o = (Letter) other;
    return unknownFields().equals(o.unknownFields());
  }

  @Override
  public int hashCode() {
    return unknownFields().hashCode();
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    return builder.replace(0, 2, "Letter{").append('}').toString();
  }

  public static final class Builder extends Message.Builder<Letter, Builder> {
    public Builder() {
    }

    @Override
    public Letter build() {
      return new Letter(super.buildUnknownFields());
    }
  }

  private static final class ProtoAdapter_Letter extends ProtoAdapter<Letter> {
    public ProtoAdapter_Letter() {
      super(FieldEncoding.LENGTH_DELIMITED, Letter.class);
    }

    @Override
    public int encodedSize(Letter value) {
      return value.unknownFields().size();
    }

    @Override
    public void encode(ProtoWriter writer, Letter value) throws IOException {
      writer.writeBytes(value.unknownFields());
    }

    @Override
    public Letter decode(ProtoReader reader) throws IOException {
      Builder builder = new Builder();
      long token = reader.beginMessage();
      for (int tag; (tag = reader.nextTag()) != -1;) {
        switch (tag) {
          default: {
            reader.readUnknownField(tag);
          }
        }
      }
      builder.addUnknownFields(reader.endMessageAndGetUnknownFields(token));
      return builder.build();
    }

    @Override
    public Letter redact(Letter value) {
      Builder builder = value.newBuilder();
      builder.clearUnknownFields();
      return builder.build();
    }
  }
}
