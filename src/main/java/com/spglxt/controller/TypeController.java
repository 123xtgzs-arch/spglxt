package com.spglxt.controller;

import com.spglxt.common.Result;
import com.spglxt.entity.Type;
import com.spglxt.service.TypeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 分类管理控制器
 * 
 * @author spglxt
 */
@Slf4j
@RestController
@RequestMapping("/api/type")
@RequiredArgsConstructor
public class TypeController {

    private final TypeService typeService;

    /**
     * 获取分类树形列表
     *
     * @param typeMid 模型ID (默认1=视频)
     * @return 分类树
     */
    @GetMapping("/tree")
    public Result<List<Type>> getTypeTree(@RequestParam(defaultValue = "1") Integer typeMid) {
        List<Type> typeTree = typeService.getTypeTree(typeMid);
        return Result.success(typeTree);
    }

    /**
     * 获取所有分类 (平铺列表)
     *
     * @param typeMid 模型ID (默认1=视频)
     * @return 分类列表
     */
    @GetMapping("/list")
    public Result<List<Type>> getAllTypes(@RequestParam(defaultValue = "1") Integer typeMid) {
        List<Type> types = typeService.getAllTypes(typeMid);
        return Result.success(types);
    }

    /**
     * 获取分类详情
     *
     * @param id 分类ID
     * @return 分类详情
     */
    @GetMapping("/{id}")
    public Result<Type> getType(@PathVariable Integer id) {
        return typeService.getTypeById(id)
                .map(Result::success)
                .orElse(Result.error("分类不存在"));
    }

    /**
     * 添加分类
     *
     * @param type 分类信息
     * @return 添加结果
     */
    @PostMapping
    public Result<Type> addType(@RequestBody Type type) {
        Type savedType = typeService.addType(type);
        return Result.success(savedType);
    }

    /**
     * 更新分类
     *
     * @param id   分类ID
     * @param type 分类信息
     * @return 更新结果
     */
    @PutMapping("/{id}")
    public Result<Type> updateType(@PathVariable Integer id, @RequestBody Type type) {
        Type updatedType = typeService.updateType(id, type);
        return Result.success(updatedType);
    }

    /**
     * 删除分类
     *
     * @param id 分类ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteType(@PathVariable Integer id) {
        typeService.deleteType(id);
        return Result.success("删除成功");
    }

    /**
     * 获取分类下的视频数量
     *
     * @param id 分类ID
     * @return 视频数量
     */
    @GetMapping("/{id}/count")
    public Result<Long> getVideoCount(@PathVariable Integer id) {
        Long count = typeService.getVideoCount(id);
        return Result.success(count);
    }
}
