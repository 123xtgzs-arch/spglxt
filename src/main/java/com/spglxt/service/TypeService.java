package com.spglxt.service;

import com.spglxt.entity.Type;
import com.spglxt.repository.TypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 分类服务层
 * 
 * @author spglxt
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TypeService {

    private final TypeRepository typeRepository;

    /**
     * 获取所有分类 (树形结构)
     *
     * @param typeMid 模型ID (1=视频)
     * @return 树形结构分类列表
     */
    public List<Type> getTypeTree(Integer typeMid) {
        List<Type> allTypes = typeRepository.findByTypeMidOrderByTypeSortAscTypeIdAsc(typeMid);
        return buildTree(allTypes, 0);
    }

    /**
     * 构建树形结构
     *
     * @param allTypes 所有分类列表
     * @param pid      父级ID
     * @return 树形结构列表
     */
    private List<Type> buildTree(List<Type> allTypes, Integer pid) {
        List<Type> tree = new ArrayList<>();
        for (Type type : allTypes) {
            if (type.getTypePid().equals(pid)) {
                List<Type> children = buildTree(allTypes, type.getTypeId());
                type.setChildren(children);
                tree.add(type);
            }
        }
        return tree;
    }

    /**
     * 获取所有分类 (平铺列表)
     *
     * @param typeMid 模型ID (1=视频)
     * @return 平铺分类列表
     */
    public List<Type> getAllTypes(Integer typeMid) {
        return typeRepository.findByTypeMidOrderByTypeSortAscTypeIdAsc(typeMid);
    }

    /**
     * 根据ID查询分类
     *
     * @param id 分类ID
     * @return 分类对象
     */
    public Optional<Type> getTypeById(Integer id) {
        return typeRepository.findById(id);
    }

    /**
     * 根据名称查询分类
     *
     * @param typeName 分类名称
     * @return 分类对象
     */
    public Type getTypeByName(String typeName) {
        return typeRepository.findByTypeName(typeName);
    }

    /**
     * 添加分类
     *
     * @param type 分类对象
     * @return 保存后的分类对象
     */
    @Transactional
    public Type addType(Type type) {
        return typeRepository.save(type);
    }

    /**
     * 更新分类
     *
     * @param id   分类ID
     * @param type 更新的分类信息
     * @return 更新后的分类对象
     */
    @Transactional
    public Type updateType(Integer id, Type type) {
        return typeRepository.findById(id).map(existingType -> {
            type.setTypeId(id);
            return typeRepository.save(type);
        }).orElseThrow(() -> new RuntimeException("分类不存在: " + id));
    }

    /**
     * 删除分类
     *
     * @param id 分类ID
     */
    @Transactional
    public void deleteType(Integer id) {
        typeRepository.deleteById(id);
    }

    /**
     * 获取分类下的视频数量
     *
     * @param typeId 分类ID
     * @return 视频数量
     */
    public Long getVideoCount(Integer typeId) {
        return typeRepository.countVideosByTypeId(typeId);
    }
}
