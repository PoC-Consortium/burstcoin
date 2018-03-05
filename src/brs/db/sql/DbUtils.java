package brs.db.sql;

import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.impl.TableImpl;
import org.jooq.Field;
import org.jooq.SelectQuery;

public final class DbUtils {

  private static final Logger logger = LoggerFactory.getLogger(DbUtils.class);
  private DbUtils() {
  } // never

  public static void close(AutoCloseable... closeables) {
    for (AutoCloseable closeable : closeables) {
      if (closeable != null) {
        try {
          closeable.close();
        } catch (Exception ignore) {
        }
      }
    }
  }

  public static void applyLimits(SelectQuery query, int from, int to ) {
    int limit = to >= 0 && to >= from && to < Integer.MAX_VALUE ? to - from + 1 : 0;
    if (limit > 0 && from > 0) {
      query.addLimit(from, limit);
    }
    else if (limit > 0) {
      query.addLimit(limit);
    }
    else if (from > 0) {
      query.addOffset(from);
    }
  }

  public static void mergeInto(DSLContext ctx, Record record, TableImpl table, Field[] keyFields) {
    // this is a hack .. we ignore always the first column on mergeInto commands to not fall over the db_id key
    ctx.mergeInto(table, Arrays.copyOfRange(record.fields(), 1, record.fields().length))
        .key(keyFields).values(Arrays.copyOfRange(record.valuesRow().fields(), 1, record.valuesRow().fields().length))
        .execute();
  }
}
