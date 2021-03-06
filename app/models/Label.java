package models;

import models.enumeration.ResourceType;
import models.resource.Resource;
import play.data.validation.Constraints.Required;
import play.db.ebean.Model;

import javax.persistence.*;
import java.util.Set;

/**
 * 프로젝트에 붙일 수 있는 라벨
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"category", "name"}))
public class Label extends Model implements ResourceConvertible {

    /**
     *
     */
    private static final long serialVersionUID = -35487506476718498L;
    public static final Finder<Long, Label> find = new Finder<>(Long.class, Label.class);

    @Id
    public Long id;

    @Required
    public String category;

    @Required
    public String name;

    @ManyToMany(mappedBy="labels")
    public Set<Project> projects;

    /**
     * 주어진 {@code category}와 {@code name}으로 라벨를 생성한다.
     * @param category 이 라벨가 속한 분류
     * @param name 이 라벨의 이름
     */
    public Label(String category, String name) {
        if (category == null) {
            category = "Label";
        }
        this.category = category;
        this.name = name;
    }

    /**
     * 현재 인스턴스와 같은 라벨가 있는지의 여부를 반환한다.
     *
     * when: 사용자가 라벨를 추가하려고 했을 때, 중복 여부를 검사하고자 할 때 사용하고 있다.
     *
     * 이 인스턴스의 {@link Label#category}와 {@link Label#name}가 모두 같은 라벨가 DB에 존재하는지 확인한다.
     *
     * @return 같은 것이 존재하면 {@code true}, 아니면 {@code false}
     */
    @Transient
    public boolean exists() {
        return find.where().eq("category", category).eq("name", name)
            .findRowCount() > 0;
    }

    /**
     * 라벨를 삭제한다.
     *
     * 모든 프로젝트에서 이 라벨를 제거한 뒤, 라벨를 삭제한다.
     */
    @Override
    public void delete() {
        for(Project project: projects) {
            project.labels.remove(this);
            project.update();
        }
        super.delete();
    }


    /**
     * 라벨를 문자열로 변환하여 반환한다.
     *
     * {@link Label#category}와 {@link Label#name}을 " - "로 연결한 문자열을 반환한다.
     *
     * @return "{@link Label#category} - {@link Label#name}" 형식의 문자열
     */
    @Override
    public String toString() {
        return category + " - " + name;
    }

    /**
     * 라벨를 {@link Resource} 형식으로 반환한다.
     *
     * when: 이 라벨에 대해 접근권한이 있는지 검사하기 위해 {@link utils.AccessControl}에서 사용한다.
     *
     * @return {@link Resource}로서의 라벨
     */
    @Override
    public Resource asResource() {
        return new Resource() {
            @Override
            public String getId() {
                return id.toString();
            }

            @Override
            public Project getProject() {
                return null;
            }

            @Override
            public ResourceType getType() {
                return ResourceType.LABEL;
            }
        };
    }

    public void delete(Project project) {
        this.projects.remove(project);
    }
}
