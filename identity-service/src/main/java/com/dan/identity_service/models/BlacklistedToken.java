package com.dan.identity_service.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Document(collection = "blacklisted_tokens")
@CompoundIndexes({
        @CompoundIndex(name = "token_index", def = "{'token': 1}", unique = true),
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class BlacklistedToken {
    @Id
    String id;
    String token;
    Date expirationDate;
}
