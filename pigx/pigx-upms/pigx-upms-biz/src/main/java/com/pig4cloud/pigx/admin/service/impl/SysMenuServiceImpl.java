/*
 *
 *      Copyright (c) 2018-2025, lengleng All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice,
 *  this list of conditions and the following disclaimer.
 *  Redistributions in binary form must reproduce the above copyright
 *  notice, this list of conditions and the following disclaimer in the
 *  documentation and/or other materials provided with the distribution.
 *  Neither the name of the pig4cloud.com developer nor the names of its
 *  contributors may be used to endorse or promote products derived from
 *  this software without specific prior written permission.
 *  Author: lengleng (wangiegie@gmail.com)
 *
 */

package com.pig4cloud.pigx.admin.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pig4cloud.pigx.admin.api.dto.MenuTree;
import com.pig4cloud.pigx.admin.api.entity.SysMenu;
import com.pig4cloud.pigx.admin.api.entity.SysRoleMenu;
import com.pig4cloud.pigx.admin.api.vo.MenuVO;
import com.pig4cloud.pigx.admin.api.util.TreeUtil;
import com.pig4cloud.pigx.admin.mapper.SysMenuMapper;
import com.pig4cloud.pigx.admin.mapper.SysRoleMenuMapper;
import com.pig4cloud.pigx.admin.service.SysMenuService;
import com.pig4cloud.pigx.common.core.constant.CacheConstants;
import com.pig4cloud.pigx.common.core.constant.CommonConstants;
import com.pig4cloud.pigx.common.core.constant.enums.MenuTypeEnum;
import com.pig4cloud.pigx.common.core.util.R;
import lombok.AllArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * <p>
 * ??????????????? ???????????????
 * </p>
 *
 * @author lengleng
 * @since 2017-10-29
 */
@Service
@AllArgsConstructor
public class SysMenuServiceImpl extends ServiceImpl<SysMenuMapper, SysMenu> implements SysMenuService {

	private final SysRoleMenuMapper sysRoleMenuMapper;

	@Override
	@Cacheable(value = CacheConstants.MENU_DETAILS, key = "#roleId", unless = "#result.isEmpty()")
	public List<MenuVO> findMenuByRoleId(Integer roleId) {
		return baseMapper.listMenusByRoleId(roleId);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	@CacheEvict(value = CacheConstants.MENU_DETAILS, allEntries = true)
	public R removeMenuById(Integer id) {
		// ???????????????????????????????????????
		List<SysMenu> menuList = this.list(Wrappers.<SysMenu>query().lambda().eq(SysMenu::getParentId, id));
		if (CollUtil.isNotEmpty(menuList)) {
			return R.failed("??????????????????????????????");
		}

		sysRoleMenuMapper.delete(Wrappers.<SysRoleMenu>query().lambda().eq(SysRoleMenu::getMenuId, id));
		// ?????????????????????????????????
		return R.ok(this.removeById(id));
	}

	@Override
	@CacheEvict(value = CacheConstants.MENU_DETAILS, allEntries = true)
	public Boolean updateMenuById(SysMenu sysMenu) {
		return this.updateById(sysMenu);
	}

	/**
	 * ??????????????? 1. ???????????????????????????????????? 2. ?????????????????????parentId ?????? 2.1 ???????????????????????????ID -1
	 * @param lazy ??????????????????
	 * @param parentId ?????????ID
	 * @return
	 */
	@Override
	public List<MenuTree> treeMenu(boolean lazy, Integer parentId) {
		if (!lazy) {
			return TreeUtil.buildTree(
					baseMapper.selectList(Wrappers.<SysMenu>lambdaQuery().orderByAsc(SysMenu::getSort)),
					CommonConstants.MENU_TREE_ROOT_ID);
		}

		Integer parent = parentId == null ? CommonConstants.MENU_TREE_ROOT_ID : parentId;
		return TreeUtil.buildTree(
				baseMapper.selectList(
						Wrappers.<SysMenu>lambdaQuery().eq(SysMenu::getParentId, parent).orderByAsc(SysMenu::getSort)),
				parent);
	}

	/**
	 * ????????????
	 * @param all ????????????
	 * @param type ??????
	 * @param parentId ?????????ID
	 * @return
	 */
	@Override
	public List<MenuTree> filterMenu(Set<MenuVO> all, String type, Integer parentId) {
		List<MenuTree> menuTreeList = all.stream().filter(menuTypePredicate(type)).map(MenuTree::new)
				.sorted(Comparator.comparingInt(MenuTree::getSort)).collect(Collectors.toList());
		Integer parent = parentId == null ? CommonConstants.MENU_TREE_ROOT_ID : parentId;
		return TreeUtil.build(menuTreeList, parent);
	}

	/**
	 * menu ????????????
	 * @param type ??????
	 * @return Predicate
	 */
	private Predicate<MenuVO> menuTypePredicate(String type) {
		return vo -> {
			if (MenuTypeEnum.TOP_MENU.getDescription().equals(type)) {
				return MenuTypeEnum.TOP_MENU.getType().equals(vo.getType());
			}
			// ???????????? ?????? + ??????
			return !MenuTypeEnum.BUTTON.getType().equals(vo.getType());
		};
	}

}
