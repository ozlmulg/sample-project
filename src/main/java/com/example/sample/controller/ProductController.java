package com.example.sample.controller;

import com.example.sample.entity.tables.pojos.Product;
import com.example.sample.repository.ProductRepository;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.QueryValue;
import jakarta.inject.Inject;
import reactor.core.publisher.Mono;

import static java.util.Optional.ofNullable;

@Controller("/products")
public class ProductController {

  @Inject
  private ProductRepository productRepository;

  /**
   * dslContext.select works with jdbc
   */
  @Get("/jdbc/{id}")
  public Mono<Product> getByIdWithJdbc(@PathVariable long id) {
    return productRepository.getByIdWithJdbc(id);
  }

  /**
   * dslContext.select works with r2dbc
   */
  @Get("/r2dbc/{id}")
  public Mono<Product> getByIdWithR2dbc(@PathVariable long id) {
    return productRepository.getByIdWithR2dbc(id);
  }

  /**
   * dslContext.setLocal does not work with jdbc.
   * dslContext.insert works successfully.
   * We can see updated product on product table.
   * But we don't see changed_by column on product_changelog table is empty
   * even though we set it in dslContext.transactionResult
   */
  @Get("/jdbc/upsert/{id}")
  public Mono<Product> upsertWithJdbc(@PathVariable long id,
                                      @QueryValue String name,
                                      @QueryValue String userId) {
    return productRepository.upsertWithJdbc(id, name, ofNullable(userId));
  }

  /**
   * dslContext.setLocal does not work with r2dbc.
   * dslContext.insert does not work with r2dbc.
   * It does not commit the scripts to database.
   * We can not see any updated product on product table or changelog on product_changelog table.
   */
  @Get("/r2dbc/upsert/{id}")
  public Mono<Product> upsertWithR2dbc(@PathVariable long id,
                                       @QueryValue String name,
                                       @QueryValue String userId) {
    return productRepository.upsertWithR2dbc(id, name, ofNullable(userId));
  }

  /**
   * dslContext.begin works with jdbc for both of setLocal and insert statements.
   * We can see updated product on product table.
   * We can see changed_by column as userId parameter on product_changelog table.
   * But we will always need to fetch the product from table because dslContext.begin don't return updated record.
   */
  @Get("/jdbc/begin/upsert/{id}")
  public Mono<Product> upsertWithJdbcBegin(@PathVariable long id,
                                           @QueryValue String name,
                                           @QueryValue String userId) {
    return productRepository.upsertWithJdbcBegin(id, name, ofNullable(userId));
  }

  /**
   * dslContext.begin works with r2dbc for both of setLocal and insert statements.
   * We can see updated product on product table.
   * We can see changed_by column as userId parameter on product_changelog table.
   * But we will always need to fetch the product from table because dslContext.begin don't return updated record.
   */
  @Get("/r2dbc/begin/upsert/{id}")
  public Mono<Product> upsertWithR2dbcBegin(@PathVariable long id,
                                            @QueryValue String name,
                                            @QueryValue String userId) {
    return productRepository.upsertWithR2dbcBegin(id, name, ofNullable(userId));
  }
}
