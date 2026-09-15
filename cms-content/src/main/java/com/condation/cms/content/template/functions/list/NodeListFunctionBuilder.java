package com.condation.cms.content.template.functions.list;

/*-
 * #%L
 * CMS Content
 * %%
 * Copyright (C) 2023 - 2026 CondationCMS
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */
import com.condation.cms.api.Constants;
import com.condation.cms.api.db.ContentNode;
import com.condation.cms.api.db.Page;
import com.condation.cms.api.feature.features.ContentNodeMapperFeature;
import com.condation.cms.api.model.ListNode;
import com.condation.cms.api.request.RequestContext;
import com.condation.cms.api.repository.ContentRepository;
import com.condation.cms.api.utils.NodeUtil;
import com.condation.cms.content.template.functions.AbstractCurrentNodeFunction;
import java.util.Comparator;
import java.util.Objects;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author t.marx
 */
@Slf4j
public class NodeListFunctionBuilder extends AbstractCurrentNodeFunction {

	int page = Constants.DEFAULT_PAGE;
	int size = Constants.DEFAULT_PAGE_SIZE;
	int excerptLength = Constants.DEFAULT_EXCERPT_LENGTH;

	String from = "";

	boolean index = true;

	String sort = "title";
	boolean reverse = false;

	String contentType = Constants.DEFAULT_CONTENT_TYPE;

	final NodeListFunction nodeListFunction;

	final NodeListFunction nodeListFunctionNoIndex;

	public final Comparator<ContentNode> nameComparator = (node1, node2) -> {
		var filename1 = NodeUtil.getName(node1);
		var filename2 = NodeUtil.getName(node2);
		if (filename1.equals("index.md")) {
			return -1;
		} else if (filename2.equals("index.md")) {
			return 1;
		}
		return filename1.compareTo(filename2);
	};

	public NodeListFunctionBuilder(ContentRepository contentRepository,
			ContentNode currentNode, RequestContext context) {
		super(
				currentNode,
				contentRepository,
				context);
		this.nodeListFunction = new NodeListFunction(contentRepository, currentNode, context);
		this.nodeListFunctionNoIndex = new NodeListFunction(
				contentRepository, currentNode, context, true);
	}

	public NodeListFunctionBuilder from(String from) {
		this.from = from;
		return this;
	}

	public NodeListFunctionBuilder contentType(String contentType) {
		this.contentType = contentType;
		return this;
	}

	public NodeListFunctionBuilder json() {
		this.contentType = "application/json";
		return this;
	}

	public NodeListFunctionBuilder excerpt(long length) {
		this.excerptLength = (int) length;
		return this;
	}

	public NodeListFunctionBuilder page(long page) {
		this.page = (int) page;
		return this;
	}

	public NodeListFunctionBuilder page(String page) {
		this.page = Integer.parseInt(page.trim());
		return this;
	}

	public NodeListFunctionBuilder size(long size) {
		this.size = (int) size;
		return this;
	}

	public NodeListFunctionBuilder size(String size) {
		this.size = Integer.parseInt(size.trim());
		return this;
	}

	public NodeListFunctionBuilder index(boolean index) {
		this.index = index;
		return this;
	}

	public NodeListFunctionBuilder sort(String sort) {
		this.sort = sort;
		return this;
	}

	public NodeListFunctionBuilder reverse(boolean reverse) {
		this.reverse = reverse;
		return this;
	}

	public Page<ListNode> list() {
		NodeListFunction function = nodeListFunction;
		if (!index) {
			function = nodeListFunctionNoIndex;
		}

		Comparator<ContentNode> comparator = getComparator();
		if (reverse) {
			comparator = comparator.reversed();
		}

		return function.list(from, page, size, excerptLength, comparator, NodeUtil.contentTypeFiler(contentType));
	}

	private Comparator<ContentNode> getComparator() {
		if (sort == null || "name".equals(sort)) {
			return nameComparator;
		} else {
			// Comparator für die extrahierten Werte
			Comparator<Object> valueComparator = Comparator.nullsLast((Object o1, Object o2) -> {
				if (Objects.equals(o1, o2)) {
					return 0;
				}
				// null-Handling
				if (o1 == null) {
					return -1;
				}
				if (o2 == null) {
					return 1;
				}
				// falls beides Strings, case‐insensitive vergleichen
				if (o1 instanceof String && o2 instanceof String) {
					return ((String) o1).compareToIgnoreCase((String) o2);
				}
				// sonst normaler Comparable‐Vergleich
				return ((Comparable) o1).compareTo(o2);
			});

			// Comparator auf den ContentNode anwenden
			return Comparator.comparing(
					(ContentNode node) -> node.data().get(sort),
					valueComparator
			);
		}
	}
}
