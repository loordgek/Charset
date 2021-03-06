/*
 * Copyright (c) 2015-2016 Adrian Siekierka
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package pl.asie.charset.storage;

import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.client.model.IRetexturableModel;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.model.TRSRTransformation;
import net.minecraftforge.fml.client.registry.IRenderFactory;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import org.lwjgl.input.Keyboard;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;

import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import pl.asie.charset.lib.ModCharsetLib;
import pl.asie.charset.lib.utils.RenderUtils;
import pl.asie.charset.storage.barrel.*;
import pl.asie.charset.storage.locking.EntityLock;
import pl.asie.charset.storage.locking.ItemLock;
import pl.asie.charset.storage.locking.RenderLock;

public class ProxyClient extends ProxyCommon {
	public static final KeyBinding backpackOpenKey = new KeyBinding("key.charset.backpackOpen", Keyboard.KEY_C, "key.categories.gameplay");
	public static final BarrelModel barrelModel = new BarrelModel();

	@Override
	public void preInit() {
		RenderingRegistry.registerEntityRenderingHandler(EntityLock.class, new IRenderFactory<EntityLock>() {
			@Override
			public Render<? super EntityLock> createRenderFor(RenderManager manager) {
				return new RenderLock(manager);
			}
		});

		RenderingRegistry.registerEntityRenderingHandler(EntityMinecartDayBarrel.class, new IRenderFactory<EntityMinecartDayBarrel>() {
			@Override
			public Render<? super EntityMinecartDayBarrel> createRenderFor(RenderManager manager) {
				return new RenderMinecartDayBarrel(manager);
			}
		});
	}

	@Override
	public void init() {
		ClientRegistry.bindTileEntitySpecialRenderer(TileEntityDayBarrel.class, new TileEntityDayBarrelRenderer());
		ClientRegistry.registerKeyBinding(backpackOpenKey);

		Minecraft.getMinecraft().getItemColors().registerItemColorHandler(new ItemLock.Color(), ModCharsetStorage.keyItem);
		Minecraft.getMinecraft().getItemColors().registerItemColorHandler(new ItemLock.Color(), ModCharsetStorage.lockItem);

		Minecraft.getMinecraft().getBlockColors().registerBlockColorHandler(barrelModel.colorizer, ModCharsetStorage.barrelBlock);
		Minecraft.getMinecraft().getItemColors().registerItemColorHandler(barrelModel.colorizer, ModCharsetStorage.barrelItem);
		Minecraft.getMinecraft().getItemColors().registerItemColorHandler(new ItemMinecartDayBarrel.Color(), ModCharsetStorage.barrelCartItem);
	}

	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public void onKey(InputEvent.KeyInputEvent event) {
		Minecraft mc = Minecraft.getMinecraft();
		EntityPlayer player = mc.player;
		if (!mc.inGameHasFocus || player == null) {
			return;
		}
	}

	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public void onTextureMap(TextureStitchEvent.Pre event) {
		BarrelModel.onTextureLoad(event.getMap());
	}

	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public void onPostBake(ModelBakeEvent event) {
		event.getModelRegistry().putObject(new ModelResourceLocation("charsetstorage:barrel", "normal"), barrelModel);
		event.getModelRegistry().putObject(new ModelResourceLocation("charsetstorage:barrel", "inventory"), barrelModel);

		BarrelModel.template = (IRetexturableModel) RenderUtils.getModel(new ResourceLocation("charsetstorage:block/barrel"));
	}
}
