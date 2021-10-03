package org.ssssssss.magicapi.modules.table;

import org.apache.commons.lang3.StringUtils;
import org.ssssssss.magicapi.exception.MagicAPIException;
import org.ssssssss.magicapi.model.Page;
import org.ssssssss.magicapi.modules.BoundSql;
import org.ssssssss.magicapi.modules.SQLModule;
import org.ssssssss.script.annotation.Comment;

import java.io.Serializable;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 单表操作API
 *
 * @author mxd
 */
public class NamedTable {

	String tableName;

	SQLModule sqlModule;

	String primary;

	String logicDeleteColumn;

	Object logicDeleteValue;

	Map<String, Object> columns = new HashMap<>();

	List<String> fields = new ArrayList<>();

	List<String> groups = new ArrayList<>();

	List<String> orders = new ArrayList<>();

	Set<String> excludeColumns = new HashSet<>();

	Function<String, String> rowMapColumnMapper;

	Object defaultPrimaryValue;

	boolean useLogic = false;

	boolean withBlank = false;

	Where where = new Where(this);

	public NamedTable(String tableName, SQLModule sqlModule, Function<String, String> rowMapColumnMapper) {
		this.tableName = tableName;
		this.sqlModule = sqlModule;
		this.rowMapColumnMapper = rowMapColumnMapper;
		this.logicDeleteColumn = sqlModule.getLogicDeleteColumn();
		String deleteValue = sqlModule.getLogicDeleteValue();
		this.logicDeleteValue = deleteValue;
		if (deleteValue != null) {
			boolean isString = deleteValue.startsWith("'") || deleteValue.startsWith("\"");
			if (isString && deleteValue.length() > 2) {
				this.logicDeleteValue = deleteValue.substring(1, deleteValue.length() - 1);
			} else {
				try {
					this.logicDeleteValue = Integer.parseInt(deleteValue);
				} catch (NumberFormatException e) {
					this.logicDeleteValue = deleteValue;
				}
			}
		}
	}

	private NamedTable() {
	}

	@Override
	@Comment("克隆")
	public NamedTable clone() {
		NamedTable namedTable = new NamedTable();
		namedTable.tableName = this.tableName;
		namedTable.sqlModule = this.sqlModule;
		namedTable.primary = this.primary;
		namedTable.logicDeleteValue = this.logicDeleteValue;
		namedTable.logicDeleteColumn = this.logicDeleteColumn;
		namedTable.columns = new HashMap<>(this.columns);
		namedTable.fields = new ArrayList<>(fields);
		namedTable.groups = new ArrayList<>(groups);
		namedTable.orders = new ArrayList<>(orders);
		namedTable.excludeColumns = new HashSet<>(excludeColumns);
		namedTable.rowMapColumnMapper = this.rowMapColumnMapper;
		namedTable.defaultPrimaryValue = this.defaultPrimaryValue;
		namedTable.useLogic = this.useLogic;
		namedTable.withBlank = this.withBlank;
		namedTable.where = new Where(namedTable);
		return namedTable;
	}

	@Comment("使用逻辑删除")
	public NamedTable logic() {
		this.useLogic = true;
		return this;
	}

	@Comment("更新空值")
	public NamedTable withBlank() {
		this.withBlank = true;
		return this;
	}

	@Comment("设置主键名，update时使用")
	public NamedTable primary(String primary) {
		this.primary = rowMapColumnMapper.apply(primary);
		return this;
	}

	@Comment("设置主键名，并设置默认主键值(主要用于insert)")
	public NamedTable primary(String primary, Serializable defaultPrimaryValue) {
		this.primary = rowMapColumnMapper.apply(primary);
		this.defaultPrimaryValue = defaultPrimaryValue;
		return this;
	}

	@Comment("设置主键名，并设置默认主键值(主要用于insert)")
	public NamedTable primary(String primary, Supplier<Object> defaultPrimaryValue) {
		this.primary = rowMapColumnMapper.apply(primary);
		this.defaultPrimaryValue = defaultPrimaryValue;
		return this;
	}

	@Comment("拼接where")
	public Where where() {
		return where;
	}

	@Comment("设置单列的值")
	public NamedTable column(@Comment("列名") String key, @Comment("值") Object value) {
		this.columns.put(rowMapColumnMapper.apply(key), value);
		return this;
	}

	@Comment("设置查询的列，如`columns('a','b','c')`")
	public NamedTable columns(@Comment("各项列") String... columns) {
		if (columns != null) {
			for (String column : columns) {
				column(column);
			}
		}
		return this;
	}

	@Comment("设置要排除的列")
	public NamedTable exclude(String column) {
		if (column != null) {
			excludeColumns.add(rowMapColumnMapper.apply(column));
		}
		return this;
	}

	@Comment("设置要排除的列")
	public NamedTable excludes(String... columns) {
		if (columns != null) {
			excludeColumns.addAll(Arrays.stream(columns).map(rowMapColumnMapper).collect(Collectors.toList()));
		}
		return this;
	}

	@Comment("设置要排除的列")
	public NamedTable excludes(List<String> columns) {
		if (columns != null) {
			excludeColumns.addAll(columns.stream().map(rowMapColumnMapper).collect(Collectors.toList()));
		}
		return this;
	}

	@Comment("设置查询的列，如`columns(['a','b','c'])`")
	public NamedTable columns(Collection<String> columns) {
		if (columns != null) {
			columns.stream().filter(StringUtils::isNotBlank).map(rowMapColumnMapper).forEach(this.fields::add);
		}
		return this;
	}

	@Comment("设置查询的列，如`column('a')`")
	public NamedTable column(String column) {
		if (StringUtils.isNotBlank(column)) {
			this.fields.add(this.rowMapColumnMapper.apply(column));
		}
		return this;
	}

	@Comment("拼接`order by xxx asc/desc`")
	public NamedTable orderBy(@Comment("要排序的列") String column, @Comment("`asc`或`desc`") String sort) {
		this.orders.add(rowMapColumnMapper.apply(column) + " " + sort);
		return this;
	}

	@Comment("拼接`order by xxx asc`")
	public NamedTable orderBy(@Comment("要排序的列") String column) {
		return orderBy(column, "asc");
	}

	@Comment("拼接`order by xxx desc`")
	public NamedTable orderByDesc(@Comment("要排序的列") String column) {
		return orderBy(column, "desc");
	}

	@Comment("拼接`group by`")
	public NamedTable groupBy(@Comment("要分组的列") String... columns) {
		this.groups.addAll(Arrays.stream(columns).map(rowMapColumnMapper).collect(Collectors.toList()));
		return this;
	}

	private Collection<Map.Entry<String, Object>> filterNotBlanks() {
		if (this.withBlank) {
			return this.columns.entrySet()
					.stream()
					.filter(it -> !excludeColumns.contains(it.getKey()))
					.collect(Collectors.toList());
		}
		return this.columns.entrySet()
				.stream()
				.filter(it -> StringUtils.isNotBlank(Objects.toString(it.getValue(), "")))
				.filter(it -> !excludeColumns.contains(it.getKey()))
				.collect(Collectors.toList());
	}

	@Comment("执行插入,返回主键")
	public Object insert() {
		return insert(null);
	}

	@Comment("执行插入,返回主键")
	public Object insert(@Comment("各项列和值") Map<String, Object> data) {
		if (data != null) {
			data.forEach((key, value) -> this.columns.put(rowMapColumnMapper.apply(key), value));
		}
		if (this.defaultPrimaryValue != null && StringUtils.isBlank(Objects.toString(this.columns.getOrDefault(this.primary, "")))) {
			if (this.defaultPrimaryValue instanceof Supplier) {
				this.columns.put(this.primary, ((Supplier<?>) this.defaultPrimaryValue).get());
			} else {
				this.columns.put(this.primary, this.defaultPrimaryValue);
			}
		}
		Collection<Map.Entry<String, Object>> entries = filterNotBlanks();
		if (entries.isEmpty()) {
			throw new MagicAPIException("参数不能为空");
		}
		StringBuilder builder = new StringBuilder();
		builder.append("insert into ");
		builder.append(tableName);
		builder.append("(");
		builder.append(StringUtils.join(entries.stream().map(Map.Entry::getKey).toArray(), ","));
		builder.append(") values (");
		builder.append(StringUtils.join(Collections.nCopies(entries.size(), "?"), ","));
		builder.append(")");
		return sqlModule.insert(new BoundSql(builder.toString(), entries.stream().map(Map.Entry::getValue).collect(Collectors.toList()), sqlModule), this.primary);
	}

	@Comment("执行delete语句")
	public int delete() {
		if (useLogic) {
			Map<String, Object> dataMap = new HashMap<>();
			dataMap.put(logicDeleteColumn, logicDeleteValue);
			return update(dataMap);
		}
		if (where.isEmpty()) {
			throw new MagicAPIException("delete语句不能没有条件");
		}
		StringBuilder builder = new StringBuilder();
		builder.append("delete from ");
		builder.append(tableName);
		builder.append(where.getSql());
		return sqlModule.update(new BoundSql(builder.toString(), where.getParams(), sqlModule));
	}

	@Comment("保存到表中，当主键有值时则修改，否则插入")
	public Object save() {
		return this.save(null, false);
	}

	@Comment("保存到表中，当主键有值时则修改，否则插入")
	public Object save(@Comment("各项列和值") Map<String, Object> data, @Comment("是否根据id查询有没有数据") boolean beforeQuery) {
		if (StringUtils.isBlank(this.primary)) {
			throw new MagicAPIException("请设置主键");
		}
		String primaryValue = Objects.toString(this.columns.get(this.primary), "");
		if (StringUtils.isBlank(primaryValue) && data != null) {
			primaryValue = Objects.toString(data.get(this.primary), "");
		}
		if (beforeQuery) {
			if (StringUtils.isNotBlank(primaryValue)) {
				List<Object> params = new ArrayList<>();
				params.add(primaryValue);
				Integer count = sqlModule.selectInt(new BoundSql("select count(*) count from " + this.tableName + " where " + this.primary + " = ?", params, sqlModule));
				if (count == 0) {
					return insert(data);
				}
				return update(data);
			} else {
				return insert(data);
			}
		}

		if (StringUtils.isNotBlank(primaryValue)) {
			return update(data);
		}
		return insert(data);
	}

	@Comment("保存到表中，当主键有值时则修改，否则插入")
	public Object save(boolean beforeQuery) {
		return this.save(null, beforeQuery);
	}

	@Comment("保存到表中，当主键有值时则修改，否则插入")
	public Object save(@Comment("各项列和值") Map<String, Object> data) {
		return this.save(data, false);
	}


	@Comment("执行`select`查询")
	public List<Map<String, Object>> select() {
		return sqlModule.select(buildSelect());
	}

	@Comment("执行`selectOne`查询")
	public Map<String, Object> selectOne() {
		return sqlModule.selectOne(buildSelect());
	}

	private BoundSql buildSelect() {
		StringBuilder builder = new StringBuilder();
		builder.append("select ");
		List<String> fields = this.fields.stream()
				.filter(it -> !excludeColumns.contains(it))
				.collect(Collectors.toList());
		if (fields.isEmpty()) {
			builder.append("*");
		} else {
			builder.append(StringUtils.join(fields, ","));
		}
		builder.append(" from ").append(tableName);
		List<Object> params = buildWhere(builder);
		if (!orders.isEmpty()) {
			builder.append(" order by ");
			builder.append(String.join(",", orders));
		}
		if (!groups.isEmpty()) {
			builder.append(" group by ");
			builder.append(String.join(",", groups));
		}
		BoundSql boundSql = new BoundSql(builder.toString(), params, sqlModule);
		boundSql.setExcludeColumns(excludeColumns);
		return boundSql;
	}

	private List<Object> buildWhere(StringBuilder builder) {
		List<Object> params = new ArrayList<>();
		if (!where.isEmpty()) {
			where.and();
			where.ne(useLogic, logicDeleteColumn, logicDeleteValue);
			builder.append(where.getSql());
			params.addAll(where.getParams());
		} else if (useLogic) {
			where.ne(logicDeleteColumn, logicDeleteValue);
			builder.append(where.getSql());
			params.addAll(where.getParams());
		}
		return params;
	}

	@Comment("执行分页查询")
	public Object page() {
		return sqlModule.page(buildSelect());
	}

	@Comment("执行分页查询，分页条件手动传入")
	public Object page(@Comment("限制条数") long limit, @Comment("跳过条数") long offset) {
		return sqlModule.page(buildSelect(), new Page(limit, offset));
	}

	@Comment("执行update语句")
	public int update() {
		return update(null);
	}

	@Comment("执行update语句")
	public int update(@Comment("各项列和值") Map<String, Object> data, @Comment("是否更新空值字段") boolean isUpdateBlank) {
		if (null != data) {
			data.forEach((key, value) -> this.columns.put(rowMapColumnMapper.apply(key), value));
		}
		Object primaryValue = null;
		if (StringUtils.isNotBlank(this.primary)) {
			primaryValue = this.columns.remove(this.primary);
		}
		this.withBlank = isUpdateBlank;
		List<Map.Entry<String, Object>> entries = new ArrayList<>(filterNotBlanks());
		if (entries.isEmpty()) {
			throw new MagicAPIException("要修改的列不能为空");
		}
		StringBuilder builder = new StringBuilder();
		builder.append("update ");
		builder.append(tableName);
		builder.append(" set ");
		List<Object> params = new ArrayList<>();
		for (int i = 0, size = entries.size(); i < size; i++) {
			Map.Entry<String, Object> entry = entries.get(i);
			builder.append(entry.getKey()).append(" = ?");
			params.add(entry.getValue());
			if (i + 1 < size) {
				builder.append(",");
			}
		}
		if (!where.isEmpty()) {
			builder.append(where.getSql());
			params.addAll(where.getParams());
		} else if (primaryValue != null) {
			builder.append(" where ").append(this.primary).append(" = ?");
			params.add(primaryValue);
		} else {
			throw new MagicAPIException("主键值不能为空");
		}
		return sqlModule.update(new BoundSql(builder.toString(), params, sqlModule));
	}

	@Comment("执行update语句")
	public int update(@Comment("各项列和值") Map<String, Object> data) {
		return update(data, this.withBlank);
	}

	@Comment("查询条数")
	public int count() {
		StringBuilder builder = new StringBuilder();
		builder.append("select count(1) from ").append(tableName);
		List<Object> params = buildWhere(builder);
		return sqlModule.selectInt(new BoundSql(builder.toString(), params, sqlModule));
	}

	@Comment("判断是否存在")
	public boolean exists() {
		return count() > 0;
	}

}
