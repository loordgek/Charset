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

package pl.asie.charset.lib;

import com.google.common.util.concurrent.ListenableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.network.INetHandler;
import net.minecraft.network.login.INetHandlerLoginClient;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;

import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import pl.asie.charset.lib.audio.manager.AudioStreamManager;
import pl.asie.charset.lib.audio.manager.AudioStreamManagerClient;
import pl.asie.charset.lib.material.ColorLookupHandler;
import pl.asie.charset.lib.misc.SplashTextHandler;
import pl.asie.charset.lib.render.model.ModelFactory;

public class ProxyClient extends ProxyCommon {
	// TODO 1.11
	/*
	public static final RendererWire rendererWire = new RendererWire();

	@Override
	public void drawWireHighlight(PartWire wire) {
		int lineMaskCenter = 0xFFF;
		EnumFacing[] faces = WireUtils.getConnectionsForRender(wire.location);
		for (int i = 0; i < faces.length; i++) {
			EnumFacing face = faces[i];
			if (wire.connectsAny(face)) {
				int lineMask = 0xfff;
				lineMask &= ~RenderUtils.getSelectionMask(face.getOpposite());
				RenderUtils.drawSelectionBoundingBox(wire.getSelectionBox(i + 1), lineMask);
				lineMaskCenter &= ~RenderUtils.getSelectionMask(face);
			}
		}
		if (lineMaskCenter != 0) {
			RenderUtils.drawSelectionBoundingBox(wire.getSelectionBox(0), lineMaskCenter);
		}
	}
	*/

	@Override
	public EntityPlayer getPlayer(INetHandler handler) {
		return (handler instanceof INetHandlerPlayClient || handler instanceof INetHandlerLoginClient)
				? Minecraft.getMinecraft().player : super.getPlayer(handler);
	}

	@Override
	public EntityPlayer findPlayer(MinecraftServer server, String name) {
		if (server == null) {
			if (Minecraft.getMinecraft().world != null) {
				return Minecraft.getMinecraft().world.getPlayerEntityByName(name);
			}
			return null;
		} else {
			return super.findPlayer(server, name);
		}
	}

	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public void onPostBake(ModelBakeEvent event) {
	//	event.getModelRegistry().putObject(new ModelResourceLocation("charsetlib:wire", "multipart"), rendererWire);
	//	event.getModelRegistry().putObject(new ModelResourceLocation("charsetlib:wire", "inventory"), rendererWire);
	}

	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public void onTextureStitch(TextureStitchEvent.Pre event) {
		ModelFactory.clearCaches();
		ColorLookupHandler.INSTANCE.clear();

	//	for (WireFactory factory : WireManager.REGISTRY.getValues()) {
	//		rendererWire.registerSheet(event.getMap(), factory);
	//	}
	}

	@Override
	public void init() {
		super.init();

		AudioStreamManager.INSTANCE = new AudioStreamManagerClient();
		MinecraftForge.EVENT_BUS.register(new SplashTextHandler());
	}

	@Override
	public void registerItemModel(Item item, int meta, String name) {
		if (name.contains("#")) {
			ModelLoader.setCustomModelResourceLocation(item, meta, new ModelResourceLocation(name.split("#")[0], name.split("#")[1]));
		} else {
			ModelLoader.setCustomModelResourceLocation(item, meta, new ModelResourceLocation(name, "inventory"));
		}
	}

	@Override
	public World getLocalWorld(int dim) {
		if (FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT) {
			World w = Minecraft.getMinecraft().world;
			if (w != null && w.provider.getDimension() == dim) {
				return w;
			} else {
				return null;
			}
		} else {
			return DimensionManager.getWorld(dim);
		}
	}

	@Override
	public void onServerStop() {
		super.onServerStop();

		AudioStreamManagerClient.INSTANCE.removeAll();
	}

	@Override
	public boolean isCallingFromMinecraftThread() {
		return Minecraft.getMinecraft().isCallingFromMinecraftThread();
	}

	@Override
	public ListenableFuture<Object> addScheduledTask(Runnable runnable) {
		return Minecraft.getMinecraft().addScheduledTask(runnable);
	}

	@Override
	public boolean isClient() {
		return true;
	}
}
