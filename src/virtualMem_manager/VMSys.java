package virtualMem_manager;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Scanner;

public class VMSys {
	static int[] pm = new int[524288]; // physical memory
	static int[][] tlb = new int[4][3]; // tlb: {LRU, sp, f}
	static BitMap bm = new BitMap(); // bitmap of the physical memory in frame

	public static void main(String[] args) {
		if(args.length > 0)
		{
			try {
				launch_VM((String)args[0], (String)args[1]);
			} 
			catch (Exception e) {
				e.printStackTrace();
			}
		}
		else
			launch_VM();
	}
	
	public static void launch_VM(String in1, String in2) throws Exception
	{
		init_bitmap();
		init_tlb();
		try {
			init_pm_from_file(in1);
			//read_write_from_file(in2);
			read_write_from_file_with_TLB(in2);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	public static void launch_VM()
	{
		init_bitmap();
		init_tlb();
		try {
			init_pm_from_file("Input 1.txt");
			//read_write_from_file("Input 2.txt");
			read_write_from_file_with_TLB("Input 2.txt");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	// *************************************************************************************
	// Initialization:
	// *************************************************************************************

	// Initialize physical memory from init file
	private static void init_pm_from_file(String filePath) throws FileNotFoundException {
		String input;
		File inputFile = new File(filePath);
		Scanner in = new Scanner(inputFile);

		// initialize bitmap
		init_bitmap();

		// initialize page tables
		input = in.nextLine();
		init_segment_table(input);

		// initialize data pages
		input = in.nextLine();
		init_page_table(input);

		in.close();
	}

	// Initialize segment table
	public static void init_segment_table(String input) {
		String[] command = input.split("\\s+");

		for (int i = 0; i < command.length; i += 2)
			set_segment_table(Integer.parseInt(command[i]), Integer.parseInt(command[i + 1]));
	}

	// Set segment table with s: segment and f: physical address
	private static void set_segment_table(int s, int f) {
		int frameIndex;

		// assign the page table physical address pa to the segment s
		pm[s] = f;
		if (f > 0) {
			// convert the physical address f into a frame index
			// to set the correct bit in the bitmap
			frameIndex = f / VMM.FRAME_SIZE;
			// assign 2 frames for the page table
			bm.assign_frame(frameIndex, 2);
		}
	}

	// Initialize page table
	public static void init_page_table(String input) {
		String[] command = input.split("\\s+");

		for (int i = 0; i < command.length; i += 3)
			set_page_table(Integer.parseInt(command[i]), Integer.parseInt(command[i + 1]),
					Integer.parseInt(command[i + 2]));
	}

	// Set page table with p: page, s: segment and f: physical address
	private static void set_page_table(int p, int s, int f) {
		int pageTable, frameIndex;
		// fetch the page table physical address from the segment table
		pageTable = pm[s];
		// assign the data page physical address f to the page p
		pm[pageTable + p] = f;
		if (f > 0) {
			// convert the physical address f into a frame index
			// to set the correct bit in the bitmap
			frameIndex = f / VMM.FRAME_SIZE;
			// assign 1 frames for the data page
			bm.assign_frame(frameIndex, 1);
		}
	}

	// Initialize bitmap
	private static void init_bitmap() {
		bm.init();
		bm.set_bitmap(0);
	}

	// *************************************************************************************
	// Virtual Address Extraction With Bit Manipulation:
	// *************************************************************************************

	// Extract the segment s, page p and page offset w from the virtual address
	// (String)
	private static int[] extract_VA(String raw_va) {
		int n = Integer.parseInt(raw_va);
		int[] va = { n >> 19 & 0x1FF, n >> 9 & 0x3FF, n & 0x1FF };

		// return as {s, p, w}
		return va;
	}

	// Extract the segment s, page p and page offset w from the virtual address
	// (int)
	private static int[] extract_VA(int raw_va) {
		int[] va = { raw_va >> 19 & 0x1FF, raw_va >> 9 & 0x3FF, raw_va & 0x1FF };

		// return as {s, p, w}
		return va;
	}

	// Extract the segment s, page p and page offset w from the virtual address
	// (String)
	private static int extract_sp_from_VA(String raw_va) {
		int n = Integer.parseInt(raw_va);

		return n >> 9 & 0x7FFFF;
	}

	// Extract the segment s, page p and page offset w from the virtual address
	// (int)
	private static int extract_sp_from_VA(int raw_va) {
		return raw_va >> 9 & 0x7FFFF;
	}

	// Extract the segment s and page p from TLB (String)
	private static int[] extract_s_p_from_TLB(String tlb_va) {
		int n = Integer.parseInt(tlb_va);
		int[] sp = { n >> 10 & 0x1FF, n & 0x3FF };

		return sp;
	}

	// Extract the segment s and page p from TLB (int)
	private static int[] extract_s_p_from_TLB(int tlb_va) {
		int[] sp = { tlb_va >> 10 & 0x1FF, tlb_va & 0x3FF };

		return sp;
	}

	// *************************************************************************************
	// R/W from file:
	// *************************************************************************************

	// Read/Write from file
	private static void read_write_from_file(String filePath) throws FileNotFoundException {
		String input;
		File inputFile = new File(filePath);
		Scanner in = new Scanner(inputFile);

		input = in.nextLine();
		fetch_read_write(input);

		in.close();
	}

	// Read/Write from file with TLB
	private static void read_write_from_file_with_TLB(String filePath) throws FileNotFoundException {
		String input;
		File inputFile = new File(filePath);
		Scanner in = new Scanner(inputFile);

		input = in.nextLine();
		fetch_read_write_with_TLB(input);

		in.close();
	}

	// Fetch R/W command
	private static void fetch_read_write(String input) {
		String[] command = input.split("\\s+");

		for (int i = 0; i < command.length; i += 2) {
			if (Integer.parseInt(command[i]) == 0)
				read(command[i + 1]);
			else if (Integer.parseInt(command[i]) == 1)
				write(command[i + 1]);
			System.out.print(" ");
		}
	}

	// Fetch R/W command with TLB
	private static void fetch_read_write_with_TLB(String input) {
		String[] command = input.split("\\s+");

		for (int i = 0; i < command.length; i += 2) {
			if (Integer.parseInt(command[i]) == 0)
				read_with_TLB(command[i + 1]);
			else if (Integer.parseInt(command[i]) == 1)
				write_with_TLB(command[i + 1]);
			System.out.print(" ");
		}
	}

	// *************************************************************************************
	// Read without TLB:
	// *************************************************************************************

	// Read from physical memory
	private static void read(String raw_va) {
		int[] va = extract_VA(raw_va); // -> {s, p, w}
		int s = va[0], p = va[1], w = va[2], partial_pa = sub_read(s, p);

		if (partial_pa >= 0)
			System.out.print((partial_pa + w));
	}

	// Subprocess of read from physical memory
	private static int sub_read(int s, int p) {
		// process segment table
		if (check_for_read(pm[s]) <= 0)
			return -1;
		else // process page table
		{
			if (check_for_read(pm[pm[s] + p]) <= 0)
				return -1;
			else
				return pm[pm[s] + p];
		}
	}

	// Check value in physical memory for read
	private static int check_for_read(int value) {
		switch (value) {
		case -1:
			System.out.print("pf");
			return -1;
		case 0:
			System.out.print("err");
			return 0;
		default:
			return value;
		}
	}

	// *************************************************************************************
	// Write without TLB:
	// *************************************************************************************

	// Write in physical memory
	private static void write(String raw_va) {
		int[] va = extract_VA(raw_va); // -> {s, p, w}
		int s = va[0], p = va[1], w = va[2], partial_pa = sub_write(s, p);

		if (partial_pa >= 0)
			System.out.print((partial_pa + w));
	}

	// Subprocess of write in physical memory
	private static int sub_write(int s, int p) {
		int ptIndex, dataIndex;

		// process segment table
		if (pm[s] == -1) // page fault
		{
			System.out.print("pf");
			return -1;
		} else if (pm[s] == 0) {
			// allocate new blank PT (all zeroes)
			ptIndex = allocate_frame(2);
			// update the ST entry accordingly
			pm[s] = ptIndex;
			// continue with the translation process
		}

		// process page table
		if (pm[pm[s] + p] == -1) // page fault
		{
			System.out.print("pf");
			return -1;
		} else if (pm[pm[s] + p] == 0) {
			// create a new blank page
			dataIndex = allocate_frame(1);
			// update the PT entry accordingly
			pm[pm[s] + p] = dataIndex;
			// continue with the translation process
		}
		return pm[pm[s] + p];
	}

	// Allocate a frame with the given size and return its index
	private static int allocate_frame(int size) {
		int total = VMM.FRAME_SIZE * size, index = bm.assign_frame(size) * VMM.FRAME_SIZE;
		for (int i = index; i < index + total; i++)
			pm[i] = 0;

		return index;
	}

	// *************************************************************************************
	// Access Physical Memory with TLB:
	// *************************************************************************************

	// Initialize TLB
	private static void init_tlb() {
		for (int i = 0; i < tlb.length; i++) {
			tlb[i][0] = i;
			tlb[i][1] = -1;
			tlb[i][2] = -1;
		}
	}

	// Read from physical memory with TLB
	private static void read_with_TLB(String raw_va) {
		int[] va = extract_VA(raw_va); // -> {s, p, w}
		int s = va[0], p = va[1], w = va[2], partial_pa;

		partial_pa = check_TLB(raw_va);
		if (partial_pa >= 0) // hit
			System.out.print("h " + (partial_pa + w));
		else // miss
		{
			System.out.print("m ");
			partial_pa = sub_read(s, p);
			if (partial_pa >= 0) {
				System.out.print((partial_pa + w));
				update_TLB(extract_sp_from_VA(raw_va), partial_pa);
			}
		}
	}

	// Write in physical memory with TLB
	private static void write_with_TLB(String raw_va) {
		int[] va = extract_VA(raw_va); // -> {s, p, w}
		int s = va[0], p = va[1], w = va[2];
		int partial_pa;

		partial_pa = check_TLB(raw_va);
		if (partial_pa >= 0) // hit
			System.out.print("h " + ((partial_pa + w)));
		else // miss
		{
			System.out.print("m ");
			partial_pa = sub_write(s, p);
			if (partial_pa >= 0) {
				System.out.print((partial_pa + w));
				update_TLB(extract_sp_from_VA(raw_va), partial_pa);
			}
		}
	}

	// Get physical address with TLB
	private static int check_TLB(String raw_va) {
		int sp = extract_sp_from_VA(raw_va);
		int index = search_sp_in_TLB(sp);
		int partial_pa;

		if (index >= 0) // hit
		{
			partial_pa = tlb[index][2];
			for (int i = 0; i < tlb.length; i++)
				if (tlb[i][0] > tlb[index][0])
					tlb[i][0]--;
			tlb[index][0] = 3;
			return partial_pa;
		} else // miss
			return -1;
	}

	private static void update_TLB(int sp, int partial_pa) {
		for (int i = 0; i < tlb.length; i++) {
			if (tlb[i][0] == 0) {
				tlb[i][0] = 3;
				tlb[i][1] = sp;
				tlb[i][2] = partial_pa;
			} else if (tlb[i][0] > 0)
				tlb[i][0]--;
		}
	}

	private static int search_sp_in_TLB(int sp) {
		// tlb: {LRU, sp, f}
		for (int i = 0; i < tlb.length; i++)
			if (sp == tlb[i][1])
				return i;
		return -1;
	}

}
