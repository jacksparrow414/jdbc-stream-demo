package jdbc.stream.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 对于postgresql数据库必须要指定schema，否则会报错
 * https://stackoverflow.com/questions/45782327/org-postgresql-util-psqlexception-error-column-user0-id-does-not-exist-hibe
 */
@Entity
@Table(name = "user", schema = "public")
@Getter
@Setter
public class User implements Serializable {

    @Id
    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "pass_word")
    private String password;

    @Column(name = "roles")
    private String roles;

    @Column(name = "introduction")
    private String introduction;
}
