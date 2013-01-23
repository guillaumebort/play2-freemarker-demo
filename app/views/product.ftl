<#import 'layout.ftl' as layout>

<@layout.main title="Product detail">

    <h2>${product.name}</h2>

    <img src="${product.image}">

    <p>
    	<a href="${Router.Application.index()}">Back</a> 
    </p>

</@layout.main>