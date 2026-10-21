let catalogQuery='';
let catalogVisible=18;

const baseRecipesPage=recipesPage;
recipesPage=function(){
  let list=state.recipes;
  if(state.filter==='favorites'&&state.profile)list=list.filter(r=>state.profile.favoriteRecipeIds.includes(r.id));
  if(state.filter==='safe'&&state.profile)list=list.filter(r=>!r.ingredients.some(i=>state.profile.allergyProductIds.includes(i.productId)));
  if(state.filter==='mine')list=list.filter(r=>r.ownerId===state.userId);
  const query=catalogQuery.trim().toLocaleLowerCase('ru-RU');
  if(query)list=list.filter(r=>`${recipeName(r)} ${r.ingredients.map(i=>productName(i.productId)).join(' ')}`.toLocaleLowerCase('ru-RU').includes(query));
  const shown=list.slice(0,catalogVisible);
  layout(`<div class="container page catalog-page"><div class="page-intro"><span class="eyebrow">Каталог</span><h1 class="page-title">Выберите, что приготовить</h1><p>Рецепты на каждый день. Ищите по названию или продукту, сохраняйте любимые и добавляйте свои.</p></div><div class="catalog-tools"><label class="search-field"><span aria-hidden="true">⌕</span><input id="recipe-search" type="search" autocomplete="off" placeholder="Название блюда или продукт" aria-label="Поиск рецептов" value="${esc(catalogQuery)}"></label><button class="btn" id="new-recipe" type="button">+ Свой рецепт</button></div><div class="filters"><button class="chip ${state.filter==='all'?'active':''}" data-filter="all">Все блюда</button><button class="chip ${state.filter==='safe'?'active':''}" data-filter="safe">Без аллергенов</button><button class="chip ${state.filter==='favorites'?'active':''}" data-filter="favorites">Любимые</button><button class="chip ${state.filter==='mine'?'active':''}" data-filter="mine">Мои рецепты</button></div><p class="catalog-count">Найдено: ${list.length}</p>${cards(shown)}${list.length>shown.length?'<div class="more-wrap"><button class="btn ghost" id="show-more" type="button">Показать ещё</button></div>':''}</div>`);
  const search=$('#recipe-search');
  search.oninput=()=>{const start=search.selectionStart;catalogQuery=search.value;catalogVisible=18;recipesPage();const next=$('#recipe-search');next.focus();next.setSelectionRange(start,start)};
  $('#new-recipe').onclick=()=>requireAuth()&&openRecipeDialog();
  if($('#show-more'))$('#show-more').onclick=()=>{catalogVisible+=18;recipesPage()};
};

function openDialog(title,body){
  document.querySelector('.editor-dialog')?.remove();
  const shell=document.createElement('div');shell.className='editor-dialog';
  shell.innerHTML=`<div class="dialog-shade" data-close-dialog></div><section class="dialog-panel" role="dialog" aria-modal="true" aria-label="${esc(title)}"><div class="dialog-head"><div><span class="eyebrow">Ваша кухня</span><h2>${esc(title)}</h2></div><button class="dialog-close" type="button" data-close-dialog aria-label="Закрыть">×</button></div>${body}</section>`;
  document.body.append(shell);document.body.classList.add('dialog-open');
  const close=()=>{shell.remove();document.body.classList.remove('dialog-open')};
  shell.querySelectorAll('[data-close-dialog]').forEach(button=>button.onclick=close);
  shell.onkeydown=e=>{if(e.key==='Escape')close()};
  shell.querySelector('input,select,textarea')?.focus();
  return {shell,close};
}

function openProductDialog(){
  const {shell,close}=openDialog('Новый продукт',`<p class="muted">Укажите привычный объём покупки. При добавлении в запасы его можно изменить.</p><form id="product-editor" class="editor-form"><label class="field">Название<input name="name" maxlength="80" required placeholder="Например, авокадо"></label><div class="editor-grid"><label class="field">Единица измерения<select name="unit"><option value="шт">шт</option><option value="г">г</option><option value="мл">мл</option></select></label><label class="field">Предполагаемый объём<input name="defaultQuantity" type="number" min="0.001" max="1000000" step="0.001" value="1" required></label></div><label class="toggle-field"><input name="perishable" type="checkbox"><span>У продукта есть срок годности</span></label><p class="form-error" id="product-editor-error"></p><button class="btn" type="submit">Сохранить продукт</button></form>`);
  const form=shell.querySelector('form');const qty=form.elements.defaultQuantity;
  form.elements.unit.onchange=()=>{qty.value=form.elements.unit.value==='шт'?'1':form.elements.unit.value==='г'?'200':'250'};
  form.onsubmit=async e=>{e.preventDefault();const f=new FormData(form);try{const created=await api('/products',{method:'POST',body:{name:String(f.get('name')).trim(),unit:f.get('unit'),defaultQuantity:Number(f.get('defaultQuantity')),perishable:f.has('perishable')}});await loadPublic();close();pantryPage();const select=$('#stock-form [name=productId]');select.value=created.id;select.dispatchEvent(new Event('change'));toast('Продукт добавлен')}catch(err){shell.querySelector('#product-editor-error').textContent=err.message}};
}

function ingredientRow(){return `<div class="ingredient-editor-row"><select class="ingredient-product" aria-label="Продукт">${state.products.map(p=>`<option value="${p.id}">${esc(productName(p.id))} · ${esc(p.unit)}</option>`).join('')}</select><input class="ingredient-quantity" type="number" min="0.001" max="1000000" step="0.001" aria-label="Количество" required><button type="button" class="remove-row" aria-label="Убрать продукт">×</button></div>`}
function openRecipeDialog(){
  const {shell,close}=openDialog('Свой рецепт',`<p class="muted">Добавьте ингредиенты и шаги. Рецепт сохранится в вашем каталоге.</p><form id="recipe-editor" class="editor-form"><label class="field">Название блюда<input name="title" maxlength="120" required placeholder="Например, тост с авокадо"></label><div class="editor-subhead"><h3>Ингредиенты</h3><button type="button" class="text-link" id="add-ingredient">+ Добавить продукт</button></div><div id="ingredient-rows"></div><div class="button-row"><button type="button" class="btn ghost small" id="recipe-new-product">+ Новый продукт</button></div><label class="field">Шаги приготовления<textarea name="steps" rows="7" required placeholder="Один шаг на строку"></textarea></label><p class="form-error" id="recipe-editor-error"></p><button class="btn" type="submit">Сохранить рецепт</button></form>`);
  const rows=shell.querySelector('#ingredient-rows');
  const addRow=()=>{rows.insertAdjacentHTML('beforeend',ingredientRow());const row=rows.lastElementChild;const select=row.querySelector('select');const amount=row.querySelector('input');const suggest=()=>amount.value=product(select.value)?.defaultQuantity||1;select.onchange=suggest;suggest();row.querySelector('button').onclick=()=>row.remove()};
  addRow();shell.querySelector('#add-ingredient').onclick=addRow;
  shell.querySelector('#recipe-new-product').onclick=()=>{close();openProductDialog()};
  shell.querySelector('form').onsubmit=async e=>{e.preventDefault();const form=e.target;const ingredients=[...rows.children].map(row=>({productId:row.querySelector('select').value,quantity:Number(row.querySelector('input').value)}));const steps=String(new FormData(form).get('steps')).split(/\r?\n/).map(x=>x.trim()).filter(Boolean);if(!ingredients.length||!steps.length){shell.querySelector('#recipe-editor-error').textContent='Добавьте ингредиенты и шаги';return}try{const created=await api('/recipes',{method:'POST',body:{title:String(new FormData(form).get('title')).trim(),ingredients,steps}});await loadPublic();close();toast('Рецепт сохранён');navigate('recipe/'+created.id)}catch(err){shell.querySelector('#recipe-editor-error').textContent=err.message}};
}

const basePantryPage=pantryPage;
pantryPage=function(){
  basePantryPage();
  const form=$('#stock-form');if(!form)return;
  const link=document.createElement('button');link.type='button';link.className='text-link add-product-link';link.textContent='+ Добавить свой продукт';form.after(link);link.onclick=openProductDialog;
  const select=form.elements.productId;const amount=form.elements.quantity;const suggested=()=>{amount.value=product(select.value)?.defaultQuantity||1};
  select.addEventListener('change',suggested);suggested();
};

const baseRecommendationsPage=recommendationsPage;
recommendationsPage=async function(){
  await baseRecommendationsPage();
  document.querySelectorAll('.recommendation-columns section').forEach(section=>{
    const items=[...section.querySelectorAll('.recommend-card')];
    if(items.length<=12)return;
    let visible=12;
    items.slice(visible).forEach(item=>item.hidden=true);
    const more=document.createElement('button');more.type='button';more.className='btn ghost small recommendation-more';more.textContent='Показать ещё';section.append(more);
    more.onclick=()=>{items.slice(visible,visible+12).forEach(item=>item.hidden=false);visible+=12;if(visible>=items.length)more.remove()};
  });
};
