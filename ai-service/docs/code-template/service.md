# 生成 Service 模板

## 用途

生成标准 Spring Boot Service 层。

## 模板

```java
@Service
@Transactional
public class ${EntityName}Service {

    private final ${EntityName}Repository ${entity}Repository;

    public ${EntityName}Service(${EntityName}Repository ${entity}Repository) {
        this.${entity}Repository = ${entity}Repository;
    }

    public List<${EntityName}> findAll() {
        return ${entity}Repository.findAll();
    }

    public ${EntityName} findById(Long id) {
        return ${entity}Repository.findById(id)
            .orElseThrow(() -> new NotFoundException("${EntityName} not found: " + id));
    }

    public ${EntityName} create(${EntityName} dto) {
        return ${entity}Repository.save(dto);
    }

    public ${EntityName} update(Long id, ${EntityName} dto) {
        ${EntityName} entity = findById(id);
        // BeanUtils.copyProperties(dto, entity, "id");
        return ${entity}Repository.save(entity);
    }

    public void delete(Long id) {
        ${EntityName} entity = findById(id);
        ${entity}Repository.delete(entity);
    }
}
```

## 生成规则

- 包含完整的 CRUD 方法
- findById 未找到时抛 NotFoundException
- 标注 @Transactional 确保事务
- 通过构造器注入依赖

## 相关来源

architecture > tech-stack.md