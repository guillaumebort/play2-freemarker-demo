<#import 'layout.ftl' as layout>

<@layout.main title="Home">

    <h2>${products ? size} Products</h2>

    <ul>
    <#list products as product>
        <li><a href="${Router.Application.product(product.id)}">${product.name}</a></li>
    </#list>
    </ul>

</@layout.main>