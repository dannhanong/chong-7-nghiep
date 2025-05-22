package com.dan.identity_service.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.dan.identity_service.dtos.enums.RoleName;

@Document(collection = "roles")
@CompoundIndexes({
        @CompoundIndex(name = "name_index", def = "{'name': 1}", unique = true)
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Role {
    @Id
    private String id;
    private RoleName name;
}
