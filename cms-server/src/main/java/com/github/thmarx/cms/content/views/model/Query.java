package com.github.thmarx.cms.content.views.model;

import java.util.List;
import lombok.Data;

/**
 *
 * @author t.marx
 */
@Data
public class Query {
	private String page;
	private String size;
	private String from;
	private String excerpt;
	private String order_by;
	private String order_direction;
	private List<Condition> conditions;
}
