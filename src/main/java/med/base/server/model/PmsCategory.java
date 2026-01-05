package med.base.server.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PmsCategory implements Serializable {
    private static final long serialVersionUID = -2996487306685152458L;

    @TableId(type = IdType.AUTO)
    private Integer categoryId;
    private int parentId;
    private String categoryName;
    private int level;
    private int sort;
    private String icon; // 分类图标/图片URL
}
