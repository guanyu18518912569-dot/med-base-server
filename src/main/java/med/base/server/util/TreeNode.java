package med.base.server.util;

import lombok.Data;

import java.io.Serializable;
import java.util.List;
@Data
public class TreeNode implements Serializable {

    private static final long serialVersionUID = 6688473429936615108L;

    public TreeNode(int id, int parentId,
                    String title, String href, String icon,
                    String menuType, int weight) {
        this.id = id;
        this.parentId = parentId;
        this.title = title;
        this.href = href;
        this.icon = icon;
        this.menuType = menuType;
        this.weight = weight;
    }

    private int id;
    private String title;
    private String href;
    private String icon;
    private String menuType;
    private int parentId;
    private int weight;
    private List<TreeNode> children;

    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }

    public String getHref() {
        return href;
    }
    public void setHref(String href) {
        this.href = href;
    }

    public String getIcon() {
        return icon;
    }
    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getMenuType() {
        return menuType;
    }
    public void setMenuType(String menuType) {
        this.menuType = menuType;
    }

    public int getParentId() {
        return parentId;
    }
    public void setParentId(int parentId) {
        this.parentId = parentId;
    }

    public int getWeight() {
        return weight;
    }
    public void setWeight(int weight) {
        this.weight = weight;
    }

    public List<TreeNode> getChildren() {
        return children;
    }
    public void setChildren(List<TreeNode> children) {
        this.children = children;
    }
}
