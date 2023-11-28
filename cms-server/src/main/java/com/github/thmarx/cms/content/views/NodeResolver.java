package com.github.thmarx.cms.content.views;

import com.github.thmarx.cms.api.Constants;
import com.github.thmarx.cms.api.markdown.MarkdownRenderer;
import com.github.thmarx.cms.content.ContentParser;
import com.github.thmarx.cms.content.views.model.View;
import com.github.thmarx.cms.filesystem.FileSystem;
import com.github.thmarx.cms.filesystem.query.Query;
import com.github.thmarx.cms.api.content.Node;
import com.github.thmarx.cms.template.functions.list.NodeListFunctionBuilder;
import com.github.thmarx.cms.api.content.Page;
import com.github.thmarx.cms.filesystem.query.QueryUtil;
import com.github.thmarx.cms.template.functions.query.QueryFunction;
import com.google.common.base.Strings;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.graalvm.polyglot.Context;
import org.yaml.snakeyaml.Yaml;

/**
 *
 * @author t.marx
 */
@RequiredArgsConstructor
public class NodeResolver {
	final FileSystem fileSystem;
	final Path currentNode;
	final ContentParser contentParser;
	final MarkdownRenderer markdownRenderer;
	final Context context;
	final Map<String, List<String>> queryParams;
	
	public Page<Node> nodelist (View view) {
		NodeListFunctionBuilder nodelistBuilder = new NodeListFunctionBuilder(fileSystem, currentNode, contentParser, markdownRenderer);
		
		context.getBindings("js").putMember("queryParams", queryParams);
		
		var nl = view.getContent().getNodelist();
		nodelistBuilder.excerpt(getIntValue(nl.getExcerpt(), Constants.DEFAULT_EXCERPT_LENGTH));
		nodelistBuilder.page(getIntValue(nl.getPage(), Constants.DEFAULT_PAGE));
		nodelistBuilder.size(getIntValue(nl.getSize(), Constants.DEFAULT_PAGE_SIZE));
		nodelistBuilder.from(getStringValue(nl.getFrom(), ""));
		nodelistBuilder.sort(getStringValue(nl.getSort(), "title"));
		nodelistBuilder.index(getBooleanValue(nl.getIndex(), true));
		nodelistBuilder.reverse(getBooleanValue(nl.getReverse(), false));
		
		return nodelistBuilder.list();
	}
	
	public Page<Node> query (View view) {
		QueryFunction queryFunction = new QueryFunction(fileSystem, currentNode, contentParser, markdownRenderer);
		
		context.getBindings("js").putMember("queryParams", queryParams);
		
		Query<?> query = null;
		if (!Strings.isNullOrEmpty(view.getContent().getQuery().getFrom())) {
			query = queryFunction.create(getStringValue(view.getContent().getQuery().getFrom(), ""));
		} else {
			query = queryFunction.create();
		}
		
		query.excerpt(getIntValue(view.getContent().getQuery().getExcerpt(), Constants.DEFAULT_EXCERPT_LENGTH));
		
		var order_by = getStringValue(view.getContent().getQuery().getOrder_by(), null);
		var order_direction = getStringValue(view.getContent().getQuery().getOrder_direction(), null);
		if  (!Strings.isNullOrEmpty(order_by) && !Strings.isNullOrEmpty(order_direction)) {
			if ("asc".equalsIgnoreCase(order_direction)) {
				query = query.orderby(order_by).asc();
			} else if ("desc".equalsIgnoreCase(order_direction)) {
				query = query.orderby(order_by).desc();
			}
		}
	
		if (view.getContent().getQuery().getConditions() != null && !view.getContent().getQuery().getConditions().isEmpty()) {
			for (var condition : view.getContent().getQuery().getConditions()) {
				if ("where".equalsIgnoreCase(condition.getName())) {
					var operator = condition.getOperator();
					var key = getStringValue(condition.getKey(), null);
					var value = getStringValue(condition.getValue(String.class), null);
					
					if (value != null) {
						query = query.where(key, operator, value);
					}
				} else if ("whereIn".equalsIgnoreCase(condition.getName())) {
					var key = getStringValue(condition.getKey(), null);
					var value = condition.getValue(List.class);
					if (value != null) {
						query = query.whereIn(key, value);
					}
				} else if ("whereNotIn".equalsIgnoreCase(condition.getName())) {
					var key = getStringValue(condition.getKey(), null);
					var value = condition.getValue(List.class);
					if (value != null) {
						query = query.whereNotIn(key, value);
					}
				} else if ("whereContains".equalsIgnoreCase(condition.getName())) {
					var key = getStringValue(condition.getKey(), null);
					var value = getStringValue(condition.getValue(String.class), null);
					if (value != null) {
						query = query.whereContains(key, value);
					}
				} else if ("whereNotContains".equalsIgnoreCase(condition.getName())) {
					var key = getStringValue(condition.getKey(), null);
					var value = getStringValue(condition.getValue(String.class), null);
					if (value != null) {
						query = query.whereNotContains(key, value);
					}
				}
			};
		}
		
		int page = getIntValue(view.getContent().getQuery().getPage(), Constants.DEFAULT_PAGE);
		int size = getIntValue(view.getContent().getQuery().getSize(), Constants.DEFAULT_PAGE_SIZE);
		
		return (Page<Node>) query.page(page, size);
	}
	
	private List<?> getListValue (String value, List<?> defaultValue) {		
		if (value == null) {
			return defaultValue;
		}
		if (!value.contains("queryParams.")) {
			return new Yaml().loadAs(value, List.class);
		} 
		
		throw new RuntimeException("expression not allowed in list values");
		
//		var result = context.eval("js", value);
//		return result.as(List.class);
	}
	
	private boolean getBooleanValue (String value, boolean defaultValue) {		
		if (value == null) {
			return defaultValue;
		}
		if (!value.contains("queryParams.")) {
			return Boolean.valueOf(value.trim());
		}
		
		var result = context.eval("js", "!!" + value);
		return result.asBoolean();
	}
	
	private int getIntValue (String value, int defaultValue) {		
		if (value == null) {
			return defaultValue;
		}
		if (!value.contains("queryParams.")) {
			return Integer.valueOf(value.trim());
		}
		
		var result = context.eval("js", "parseInt(" + value + ")");
		return result.asInt();
	}
	
	private String getStringValue (String value, String defaultValue) {		
		if (value == null) {
			return defaultValue;
		}
		if (!value.contains("queryParams.")) {
			return value.trim();
		}
		
		var result = context.eval("js", value);
		return result.asString();
	}
}
