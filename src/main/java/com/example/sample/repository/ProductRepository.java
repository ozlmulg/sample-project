package com.example.sample.repository;

import com.example.sample.entity.tables.pojos.Product;
import com.example.sample.entity.tables.records.ProductRecord;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.util.Optional;
import org.jooq.DSLContext;
import org.jooq.InsertOnDuplicateSetMoreStep;
import org.jooq.RowCountQuery;
import org.jooq.impl.DSL;
import reactor.core.publisher.Mono;

import static com.example.sample.entity.Tables.PRODUCT;

@Singleton
public class ProductRepository {

  public static final String SAMPLE_USER_ID = "sample.user_id";

  private final DSLContext dslContextJdbc;

  private final DSLContext dslContextR2dbc;

  @Inject
  public ProductRepository(@Named("sample-db-jdbc") DSLContext dslContextJdbc,
                           @Named("sample-db-r2dbc") DSLContext dslContextR2dbc) {
    this.dslContextJdbc = dslContextJdbc;
    this.dslContextR2dbc = dslContextR2dbc;
  }

  public Mono<Product> getByIdWithJdbc(long id) {
    return Mono.from(dslContextJdbc.selectFrom(PRODUCT)
                                   .where(PRODUCT.ID.eq(id)))
               .map(dbRecord -> dbRecord.into(Product.class));
  }

  public Mono<Product> getByIdWithR2dbc(long id) {
    return Mono.from(dslContextR2dbc.selectFrom(PRODUCT)
                                    .where(PRODUCT.ID.eq(id)))
               .map(dbRecord -> dbRecord.into(Product.class));
  }

  public Mono<Product> upsertWithJdbc(long id, String name, Optional<String> userId) {
    return dslContextJdbc.transactionResult(configuration -> {
      DSLContext ctx = DSL.using(configuration);
      return setLocalUserId(ctx, userId)
          .flatMap(result -> getInsertReturning(ctx, id, name));
    });
  }

  public Mono<Product> upsertWithR2dbc(long id, String name, Optional<String> userId) {
    return Mono.from(dslContextR2dbc.transactionPublisher(configuration -> {
      DSLContext ctx = DSL.using(configuration);
      return setLocalUserId(ctx, userId)
          .flatMap(result -> getInsertReturning(ctx, id, name));
    }));
  }

  public Mono<Product> upsertWithJdbcBegin(long id, String name, Optional<String> userId) {
    return Mono.from(dslContextJdbc.begin(getLocalUserIdQuery(dslContextJdbc, userId),
                                          getInsertQuery(dslContextJdbc, id, name)))
               .then(getByIdWithR2dbc(id));
  }

  public Mono<Product> upsertWithR2dbcBegin(long id, String name, Optional<String> userId) {
    return Mono.from(dslContextR2dbc.begin(getLocalUserIdQuery(dslContextR2dbc, userId),
                                           getInsertQuery(dslContextR2dbc, id, name)))
               .then(getByIdWithR2dbc(id));
  }

  private Mono<Product> getInsertReturning(DSLContext ctx, long productId, String name) {
    return Mono.from(getInsertQuery(ctx, productId, name)
                         .returning())
               .map(dbRecord -> dbRecord.into(Product.class));
  }

  private InsertOnDuplicateSetMoreStep<ProductRecord> getInsertQuery(DSLContext ctx, long id, String name) {
    return ctx.insertInto(PRODUCT)
              .set(PRODUCT.ID, id)
              .set(PRODUCT.NAME, name)
              .onConflict()
              .doUpdate()
              .set(PRODUCT.NAME, name);
  }

  private RowCountQuery getLocalUserIdQuery(DSLContext ctx, Optional<String> userId) {
    if (userId.isEmpty()) {
      return ctx.setLocal(DSL.name(SAMPLE_USER_ID), DSL.value(""));
    }
    return ctx.setLocal(DSL.name(SAMPLE_USER_ID), DSL.value(userId.get()));
  }

  private Mono<Integer> setLocalUserId(DSLContext ctx, Optional<String> userId) {
    if (userId.isEmpty()) {
      return Mono.just(0);
    }
    return Mono.from(ctx.setLocal(DSL.name(SAMPLE_USER_ID), DSL.value(userId.get())));
  }
}
